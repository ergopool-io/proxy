package proxy

import helpers.Helper
import io.circe.Json
import io.ebean.Ebean
import javax.inject.{Inject, Singleton}
import loggers.Logger
import models.{BlockFinder, BoxFinder}
import node._
import play.api.mvc.{RawBuffer, Request}
import pool.Pool
import proxy.status.ProxyStatus
import scalaj.http.HttpResponse

@Singleton
class Proxy @Inject()(nodeClient: NodeClient) extends ProxyConfig with LowerLayerNodeInterface {
  lazy val mnemonic: Mnemonic = new Mnemonic(nodeClient.networkType, mnemonicFilename, playSecret)
  lazy val status: ProxyStatus = new ProxyStatus
  lazy val transactionHandler: TransactionHandler = new TransactionHandler(nodeClient, lockAddress, withdrawAddress, mnemonic)

  lazy val packetHeaders: Json = Helper.convertToJson(
    s"""
      |{
      |    "addresses": {
      |        "miner": "${nodeClient.minerAddress}",
      |        "lock": "$lockAddress",
      |        "withdraw": "$withdrawAddress"
      |    },
      |    "pk": "${nodeClient.pk}"
      |}
      |""".stripMargin)
  lazy val pool: Pool = new Pool(packetBody => packetHeaders.deepMerge(packetBody).toString())

  def lockAddress: String = mnemonic.address.toString()
  lazy val withdrawAddress: String = nodeClient.deriveKey(withdraw)


  val minerPK = nodeClient.pk;
  // $COVERAGE-OFF$
  /**
   * Information of the proxy
   *
   * @return [[String]]
   */
  def info: String = {
    s"""
       |{
       |   "proxy": {
       |     "miner": {
       |        "pk": "$minerPK"
       |     },
       |     "pool": $pool,
       |     "status": $status
       |   }
       |}
       |""".stripMargin
  }
  // $COVERAGE-ON$

  /**
   * Reload pool config
   *
   * @return true if operation was a success
   */
  def reloadPoolQueueConfig(): Boolean = {
    Logger.debug("reloading pool config")
    pool.loadConfig(nodeClient.pk)
  }

  /**
   * Send solution to the pool server
   *
   * @param request [[Request]] the request that contains solution
   */
  def sendSolution(request: Request[RawBuffer]): Unit = {
    val requestBody: Json = Helper.RawBufferValue(request.body).toJson
    val cursor = requestBody.hcursor
    pool.push(Share(cursor))
  }

  /**
   * Send shares to pool
   *
   * @param shares the list of shares to send
   */
  def sendShares(shares: Vector[Share]): Unit = {
    pool.push(shares: _*)
  }

  /**
   * Get and handle mining candidate
   *
   * @return response for the miner
   */
  def getMiningCandidate: HttpResponse[Array[Byte]] = {
    val response = nodeClient.miningCandidate

    if (response.isSuccess) {
      val protectedTx = transactionHandler.getCustomTransaction(pool.transactionRequestsValue + client.transactionFee) // tx1
      val poolTx = transactionHandler.getPoolTransaction(pool.walletAddress, pool.transactionRequestsValue) // tx2

      if (protectedTx == null)
        status.protectedTx.setUnhealthy()
      else
        status.protectedTx.setHealthy()

      val body = Helper.ArrayByte(response.body).toJson
      val candidate = new Candidate(body, nodeClient, protectedTx, poolTx, pool.difficultyFactor, pool.push)

      try {
        val candidateResponse = candidate.getResponse
        status.poolTx.setHealthy()
        nodeClient.blockHeader = body.hcursor.downField("msg").as[String].getOrElse(null)
        HttpResponse[Array[Byte]](candidateResponse.toString().map(_.toByte).toArray, response.code, response.headers)
      }
      catch {
        case e: PoolTxIsNull =>
          Logger.error(s"Got PoolTxIsNull error - will set status to ${status.poolTx.color}")
          status.poolTx.setUnhealthy()
          throw e
      }
    }
    else {
      Logger.debug(s"Got Client/Server error from the node: ${Helper.ArrayByte(response.body).toString}")
      HttpResponse[Array[Byte]](response.body, response.code, response.headers)
    }
  }

  // $COVERAGE-OFF$
  /**
   * Get a method to load new boxes and blocks
   *
   * @return a function to do so
   */
  def getLoadBoxesAndBlocksMethod: Boolean => Unit = {
    val blockFinder = new BlockFinder()
    val boxFinder = new BoxFinder()

    val blockLoader = new BlockLoader(nodeClient, boxFinder, blockFinder)
    val boxLoader = new BoxLoader(nodeClient, boxFinder, blockFinder)

    loadBoxesAndBlocks(blockFinder, boxFinder, blockLoader, boxLoader)
  }
  // $COVERAGE-ON$



  /**
   * Load new boxes and blocks
   *
   * @param blockFinder the finder to get blocks
   * @param boxFinder the finder to get boxes
   * @param blockLoader loader to load new blocks
   * @param boxLoader loader to load new boxes
   */
  def loadBoxesAndBlocks(blockFinder: BlockFinder, boxFinder: BoxFinder, blockLoader: BlockLoader, boxLoader: BoxLoader)(deleteUnused: Boolean): Unit = {
    if (nodeClient.isWalletUnlocked) {
      status.walletLock.setHealthy()

      Ebean.execute(() => {
        Ebean.currentTransaction().setBatchMode(false)

        blockLoader.handleForkedBlocks()
        val latestHeight = blockFinder.maxHeight()
        val response = nodeClient.info
        if (response.isError) {
          throw NodeClientError(client.parseErrorResponse(response))
        }
        val info = Helper.ArrayByte(response.body).toJson
        val currentHeight = info.hcursor.downField("fullHeight").as[Int].getOrElse(0)

        if (currentHeight > latestHeight) {
          Logger.info(s"new blocks. loading from $latestHeight to $currentHeight")
        }

        blockLoader.loadLatestChainSlice(latestHeight, currentHeight)
        boxLoader.loadBoxes(latestHeight, currentHeight)

        if (deleteUnused) {
          Logger.info("removing unused blocks and boxes")
          blockFinder.removeOldUnusedBlocks(currentHeight)
          boxFinder.removeOldSpentBoxes(currentHeight)
        }
      })
    }
    else
      status.walletLock.setUnhealthy()
  }

  override val client: NodeClient = nodeClient
}
