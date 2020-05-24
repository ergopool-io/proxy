package node

import helpers.Helper
import io.circe.Json
import javax.inject.Singleton
import models.{Block, BlockFinder, Box, BoxFinder}
import scalaj.http.HttpResponse

@Singleton
class BoxLoader(client: NodeClient, boxFinder: BoxFinder, blockFinder: BlockFinder) {
  val chunkSize: Int = 500

  /**
   * Load boxes to the DB, old and latest
   *
   * @param maxHeight maximum blocks height in the DB
   * @param fullHeight maximum height of main chain
   */
  def loadBoxes(maxHeight: Int, fullHeight: Int): Unit = {
    var start = maxHeight

    if (maxHeight <= fullHeight - 1440) {
      val oldBoxes = loadOldUnspentBoxes(maxHeight, fullHeight - 1440, fullHeight)
      saveBoxes(oldBoxes)
      start = fullHeight - 1440
    }

    val vector = loadChunkBoxes(Math.max(start - 2, 0), fullHeight, fullHeight, (i: Int, c: Int, l: Int) => loadLatestBoxes(i, c, l))

    saveBoxes(vector)
  }

  // $COVERAGE-OFF$
  /**
   * Save a vector of boxes in the DB
   *
   * @param boxes vector of unsaved boxes
   */
  def saveBoxes(boxes: Vector[Box]): Unit = {
    boxes.foreach(box => {
      if (box.spentIn == null) {
        box.boxBytes(client)
      }
      box.save()
    })
  }
  // $COVERAGE-ON$

  /**
   * load boxes in chunk using the specified method
   *
   * @param start starting height of the boxes
   * @param end ending height of the boxes
   * @param fullHeight current full height of the main chain
   * @param method the method to call for getting each chunk
   * @return
   */
  def loadChunkBoxes(start: Int, end: Int, fullHeight: Int, method: (Int, Int, Int) => Vector[Box]): Vector[Box] = {
    var vector = Vector[Box]()
    val round: Int = (end - start) / chunkSize
    (1 to round).foreach(index => {
      val inclusion = chunkSize * (index - 1) + 1 + start
      vector ++= method(
        inclusion,
        fullHeight - chunkSize * index - start,
        inclusion + chunkSize - 1
      )
    })
    vector ++= method(chunkSize * round + 1 + start, fullHeight - end, end) // For the last iteration that has less item than chunkSize
    vector
  }

  /**
   * Load old unspent boxes and store them in DB
   *
   * @param start maximum height in database
   * @param fullHeight current full height of main chain
   */
  def loadOldUnspentBoxes(start: Int, end: Int, fullHeight: Int): Vector[Box] = {
    loadChunkBoxes(start, end, fullHeight, (i: Int, c: Int, l: Int) => fetchBoxes(i, c, l, client.fetchUnspentBoxes))
  }

  // $COVERAGE-OFF$
  /**
   * Load latest boxes and store them in DB
   *
   * @param minInclusionHeight min inclusion height of boxes
   * @param minConfirmations min confirmation of boxes
   */
  def loadLatestBoxes(minInclusionHeight: Int, minConfirmations: Int, limitInclusion: Int): Vector[Box] = {
    this.fetchBoxes(minInclusionHeight = minInclusionHeight, minConfirmations = minConfirmations, limitInclusion, client.fetchBoxes)
  }
  // $COVERAGE-ON$

  /**
   * Fetch boxes of wallet from the specified range
   *
   * @param minInclusionHeight minimum inclusionHeight of the boxes (height from bottom)
   * @param minConfirmations   minimum confirmation of the boxes (height from top)
   * @return list of boxes
   */
  def fetchBoxes(minInclusionHeight: Int = 0, minConfirmations: Int = 0, limitInclusion: Int, fetch: (Int, Int) => HttpResponse[Array[Byte]]): Vector[Box] = {
    val response = fetch(minInclusionHeight, minConfirmations)
    if (response.isError) throw NodeClientError(client.parseErrorResponse(response))

    val boxes = Helper.ArrayByte(response.body).toJson.asArray.getOrElse(Vector[Json]())

    var count = 0
    boxes.map(walletBox => {
      count = count + 1
      val cursor = walletBox.hcursor
      val boxId = cursor.downField("box").as[Json].getOrElse(Json.Null).hcursor.downField("boxId").as[String].getOrElse("")
      if ((limitInclusion != 0 && limitInclusion < cursor.downField("inclusionHeight").as[Int].getOrElse(0)) || boxFinder.boxExists(boxId))
        null
      else {
        val box = Box(cursor.downField("box").as[Json].getOrElse(Json.Null), client.networkType, cursor.downField("inclusionHeight").as[Int].getOrElse(0))

        val boxBlock = findBoxBlock(
          box.id,
          cursor.downField("creationTransaction").as[String].getOrElse(null)
        )

        if (boxBlock == null) throw new BlockNotFoundException(box.id)

        box.createdIn = boxBlock

        if (cursor.downField("spent").as[Boolean].getOrElse(false)) {
          val block = findBoxBlock(
            box.id,
            cursor.downField("spendingTransaction").as[String].getOrElse(null)
          )
          box.spentIn = block
        }

        box
      }
    }).filter(_ != null)
  }

  /**
   * Find the block that the box was created in it
   *
   * @param boxId     the box id
   * @param transactionId id of the transaction
   * @return block that the box was created in
   */
  def findBoxBlock(boxId: String, transactionId: String): Block = {
    val response = client.walletTransaction(transactionId)
    if (response.isError) throw NodeClientError(client.parseErrorResponse(response))

    val tx = Helper.ArrayByte(response.body).toJson
    val height = tx.hcursor.downField("inclusionHeight").as[Int].getOrElse(0)

    val response2 = client.blocksAtHeight(height)
    if (response2.isError) throw NodeClientError(client.parseErrorResponse(response2))
    val blocksIds = Helper.ArrayByte(response2.body).toJson
      .asArray.getOrElse(Vector[Json]())
      .flatMap(_.asString)

    val boxBlockId = {
      if (blocksIds.size == 1) blocksIds.apply(0)
      else {
        val boxBlockId = blocksIds.find(blockId => isTransactionInBlock(blockId, transactionId))

        if (boxBlockId.isDefined) boxBlockId.get else null
      }
    }

    var block = blockFinder.byBlockId(boxBlockId)

    if (block == null)
      block = Block(boxBlockId, height)

    block
  }

  /**
   * Check if the box is in one of the inputs of the block
   *
   * @param blockId the block id
   * @param transactionId   the transaction id
   * @return true if the box was created in the block, false otherwise
   */
  def isTransactionInBlock(blockId: String, transactionId: String): Boolean = {
    val response = client.blockTransactions(blockId)
    if (response.isError) throw NodeClientError(client.parseErrorResponse(response))

    val blockTransactions = Helper.ArrayByte(response.body).toJson
      .hcursor.downField("transactions").as[Json].getOrElse(Json.Null)
      .asArray.getOrElse(Vector[Json]())

    blockTransactions.exists(_.hcursor.downField("id").as[String].getOrElse(null) == transactionId)
  }

  final class BlockNotFoundException(boxId: String) extends Throwable(s"Block of $boxId not found!")
}
