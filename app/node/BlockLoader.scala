package node

import helpers.Helper
import io.circe.Json
import javax.inject.Singleton
import models.{Block, BlockFinder, Box, BoxFinder}

@Singleton
class BlockLoader(client: NodeClient, boxFinder: BoxFinder, blockFinder: BlockFinder) {
  val chunkSize: Int = 100

  /**
   * removes forked blocks, removes boxes which their created_in is in those blocks
   * sets spent_in of boxes spent in those blocks to null
   */
  def handleForkedBlocks(): Unit = {
    val intersection = this.mainChainAndDBIntersectionPoint()
    if (intersection == null) {
      blockFinder.deleteForked(0)
    } else {
      blockFinder.deleteForked(intersection.height)
    }
  }

  /**
   * loads blocks from dbHeight to currentHeight
   */
  def loadLatestChainSlice(dbHeight: Int, currentHeight: Int): Unit = {
    if (dbHeight == 0) {
      // Handle first time filling
      val blocks = fetchBlocks(currentHeight - 1440, currentHeight)
      blocks.foreach(_.save())
    }
    else {
      var fromTmp = dbHeight + 1
      while (fromTmp <= currentHeight) {
        val toTmp = Math.min(currentHeight, fromTmp + chunkSize - 1)
        val blocks = fetchBlocks(fromTmp, toTmp)
        val boxes = spentBoxes(blocks)
        blocks.foreach(_.save())
        boxes.foreach(_.save())

        fromTmp = fromTmp + chunkSize
      }

    }
  }

  /**
   * gets block headers in range
   * @param from start point
   * @param to end point
   * @return block headers in the specified range
   */
  def fetchBlocks(from: Int, to: Int): Vector[Block] = {
    val response = client.blocksRange(from, to)
    if (response.isError) {
      throw NodeClientError(client.parseErrorResponse(response))
    }

    var height = from - 1
    Helper.ArrayByte(response.body).toJson
      .asArray.getOrElse(Vector[Json]())
      .flatMap(_.asString).map(bh => {
      height += 1
      Block(bh, height)
    })
  }

  /**
   * Get those boxes which had been spent in the passed block headers list
   *
   * @param blocks list of blocks ids
   * @return boxes that had been spent
   */
  def spentBoxes(blocks: Vector[Block]): Vector[Box] = {
    blocks.flatMap(block => {
      val response = client.blockTransactions(block.blockId)
      if (response.isError) {
        throw NodeClientError(client.parseErrorResponse(response))
      }

      val cursor = Helper.ArrayByte(response.body).toJson.hcursor
      val txs = cursor
        .downField("transactions").as[Json].getOrElse(Json.Null)
        .asArray.getOrElse(Vector[Json]())

      txs.map(_.hcursor.downField("inputs").as[Json].getOrElse(Json.Null).asArray.getOrElse(Vector[Json]())) // Get input boxes
        .flatMap(_.map(_.hcursor.downField("boxId").as[String].getOrElse(null)) // Get box ids
          .flatMap(box => Option(boxFinder.byId(box))) // Find boxes in DB
          .map(box => { // Mark them as spent
            box.spentIn = block
            box
          })
        )
    })
  }

  /**
   * Find intersection of main chain and blocks in the DB
   *
   * @return the intersection block
   */
  def mainChainAndDBIntersectionPoint(): Block = {
    val ownedBlocks = blockFinder.sortedMainChainBlocks

    if (ownedBlocks.nonEmpty) {
      val maxHeight = ownedBlocks.apply(0).height

      var mainChainBlocks = blocksChunk(maxHeight)

      ownedBlocks.foreach(block => {
        val chainBlock = mainChainBlocks.get(block.height)
        if (chainBlock.isDefined) {
          if (block.blockId == chainBlock.get)
            return block
        }
        else {
          mainChainBlocks = blocksChunk(block.height)

          if (block.blockId == mainChainBlocks(block.height))
            return block
        }
      })
    }
    null
  }

  /**
   * Get a chunk of blocks with max height as maxHeight
   *
   * @param maxHeight maximum height in the chunk
   * @return mapping of height to block id
   */
  def blocksChunk(maxHeight: Int): Map[Int, String] = {
    val response = client.blocksRange(maxHeight - chunkSize + 1, maxHeight)

    if (response.isError) {
      throw NodeClientError(client.parseErrorResponse(response))
    }

    var height = maxHeight + 1
    Helper.ArrayByte(response.body).toJson
      .asArray.getOrElse(Vector[Json]())
      .reverse.flatMap(b => {
        height -= 1
        val sb = b.asString
        Option(if (sb.isDefined) height -> sb.get else null)
      }).toMap
  }
}
