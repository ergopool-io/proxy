package node

import helpers.Helper
import helpers.Helper.ArrayByte
import io.circe.{HCursor, Json}
import javax.inject.Singleton
import org.ergoplatform.appkit.{ErgoClient, NetworkType, RestApiErgoClient}
import play.api.mvc.{RawBuffer, Request}
import loggers.Logger
import models.Box
import scalaj.http.{Http, HttpResponse}

@Singleton
class NodeClient extends NodeConfig {
  type Response = HttpResponse[Array[Byte]]
  private val defaultHeader: Seq[(String, String)] = Seq[(String, String)](("Content-Type", "application/json"))
  private val authHeader: Seq[(String, String)] = Seq[(String, String)](("api_key", apiKey), ("Content-Type", "application/json"))
  var blockHeader: String = _

  lazy val minerAddress: String = deriveKey("m")

  lazy val pk: String = {
    val response = miningCandidate
    val body = ArrayByte(response.body).toJson.hcursor
    body.downField("pk").as[String].getOrElse(throw new Throwable("Error getting pk from the node: pk is null!"))
  }

  lazy val networkType: NetworkType = {
    try {
      walletAddresses.apply(0).apply(0) match {
        case '3' => NetworkType.TESTNET
        case '9' => NetworkType.MAINNET
      }
    } catch {
      case _: IndexOutOfBoundsException => throw new Throwable("Empty wallet addresses")
    }
  }

  /**
   * Check if wallet is unlocked
   *
   * @return true if wallet is unlocked
   */
  def isWalletUnlocked: Boolean = {
    val response = Http(s"$connection/wallet/status").headers(authHeader).asBytes
    Helper.ArrayByte(response.body).toJson.hcursor.downField("isUnlocked").as[Boolean].getOrElse(false)
  }

  /**
   * create new appkit node client
   *
   * @return ergoClient to work with node using appkit
   */
  def ergoClient: ErgoClient = {
    RestApiErgoClient.create(connection, networkType, apiKey)
  }

  /**
   * Get wallet addresses
   *
   * @return list of wallet addresses
   */
  def walletAddresses: Vector[String] = {
    val response = Http(s"$connection/wallet/addresses").headers(authHeader).asBytes
    Helper.ArrayByte(response.body).toJson.asArray.getOrElse(Vector[Json]()).flatMap(_.asString)
  }

  /**
   * Parse and get error message from the node response
   *
   * @param response response of the node
   * @return error message
   */
  def parseErrorResponse(response: HttpResponse[Array[Byte]]): String = {
    val body = Helper.ArrayByte(response.body).toJson
    val detail = body.hcursor.downField("detail").as[String].getOrElse("")

    val pattern = "\\([^()]*\\)".r
    var message = detail
    var newMessage = message
    while (message != newMessage) {
      newMessage = message
      message = pattern.replaceAllIn(newMessage, "")
    }
    message
  }


  /**
   * Send a transaction to the network
   *
   * @param transaction the transaction to send
   * @return response from the node
   */
  def sendErgoTransaction(transaction: Transaction): Response = {
    Http(s"$connection/transactions").postData(transaction.details.noSpaces).headers(defaultHeader).asBytes
  }

  /**
   * Get unconfirmed transactions in the network
   *
   * @param from index of first element
   * @param take number of elements in the list
   * @return list of unconfirmed transactions
   */
  def unconfirmedTransactions(from: Int, take: Int): Response = {
    Http(s"$connection/transactions/unconfirmed?limit=$take&offset=$from")
      .headers(defaultHeader)
      .asBytes
  }

  /**
   * Check if the transaction is mined
   *
   * @param transactionId [[String]] id of transaction to be checked
   * @return true if it had been mined or false otherwise
   */
  def isTransactionMined(transactionId: String): Boolean = {
    val response = Http(s"$connection/wallet/transactionById?id=$transactionId")
      .headers(authHeader).asBytes
    if (response.isSuccess) true else false
  }

  /**
   * Get last n blocks ids of main chain
   *
   * @param n number of last ids
   * @return n last blocks ids from main chain
   */
  def lastNHeaders(n: Int): Response = {
    Http(s"$connection/blocks/lastHeaders/$n").headers(defaultHeader).asBytes
  }

  /**
   * Get transactions of the block
   *
   * @param id id of the block
   * @return list of transactions of the block
   */
  def blockTransactions(id: String): Response = {
    Http(s"$connection/blocks/$id/transactions").headers(defaultHeader).asBytes
  }

  /**
   * Get the blocks full info (transactions and headers)
   *
   * @param id id of the block
   * @return full info, containing transactions and headers, of the block
   */
  def blockInfo(id: String): Response = {
    Http(s"$connection/blocks/$id").headers(defaultHeader).asBytes
  }

  /**
   * Get blocks ids at the specified height
   *
   * @param height the height to get its blocks
   * @return list of blocks ids at the specified height
   */
  def blocksAtHeight(height: Int): Response = {
    Http(s"$connection/blocks/at/$height").headers(defaultHeader).asBytes
  }

  /**
   * Get info of the wallet transaction by transactions id
   *
   * @param id id of the transaction
   * @return info of the transaction if exists
   */
  def walletTransaction(id: String): Response = {
    Http(s"$connection/wallet/transactionById?id=$id").headers(authHeader).asBytes
  }

  /**
   * Get blocks in a range of main chain
   *
   * @param fromHeight starting height of blocks
   * @param toHeight   last blocks height
   * @return list of blocks with their headers
   * Note: node returns blocks starting from (fromHeight - 1)
   */
  def blocksChainSlice(fromHeight: Int = 0, toHeight: Int = -1): Response = {
    Http(s"$connection/blocks/chainSlice?fromHeight=$fromHeight&toHeight=$toHeight")
      .headers(defaultHeader).asBytes
  }

  /**
   * Get the node info
   *
   * @return node info
   */
  def info: Response = {
    Http(s"$connection/info").asBytes
  }

  /**
   * Get range of blocks ids
   *
   * @param fromHeight starting blocks id
   * @param toHeight   last blocks id
   * @return list of blocks ids within the specified range
   */
  def blocksRange(fromHeight: Int, toHeight: Int): Response = {
    Http(s"$connection/blocks" +
      s"?limit=${toHeight - fromHeight + 1}&offset=$fromHeight")
      .headers(defaultHeader).asBytes
  }

  /**
   * Call derive key endpoint of node to get address from key
   *
   * @param key [[String]] the key derive address from
   * @return derived address
   */
  def deriveKey(key: String): String = {
    val response = Http(s"$connection/wallet/deriveKey")
      .postData(s"""{"derivationPath": "$key"}""")
      .headers(authHeader)
      .asBytes

    Helper.ArrayByte(response.body).toJson.hcursor.downField("address").as[String].getOrElse("")
  }

  /**
   * Send a request to a url with its all headers and body
   *
   * @param uri     [[String]] Servers url
   * @param request [[Request[RawBuffer]]] The request to send
   * @return Response from the the node
   */
  def sendRequest(uri: String, request: Request[RawBuffer]): Response = {
    val reqHeaders: Seq[(String, String)] = request.headers.headers

    try {
      if (request.method == "GET") {
        Http(s"$connection$uri").headers(reqHeaders).asBytes
      }
      else {
        Http(s"$connection$uri").headers(reqHeaders).postData(Helper.RawBufferValue(request.body).toString).asBytes
      }
    }
    catch {
      case error: Throwable =>
        throw new Throwable(s"Node - $uri: ${error.toString}", error)
    }
  }

  /**
   * Get latest mining candidate
   *
   * @return response from the node
   */
  def miningCandidate: Response = {
    Http(s"$connection/mining/candidate").headers(defaultHeader).asBytes
  }

  /**
   * Send solution to the node
   *
   * @param request [[Request]] the request from the miner
   * @return response of solution
   */
  def sendSolution(request: Request[RawBuffer]): Response = {
    val reqHeaders: Seq[(String, String)] = request.headers.headers
    val reqBody: HCursor = Helper.RawBufferValue(request.body).toJson.hcursor
    val body: String =
      s"""
         |{
         |  "pk": "${reqBody.downField("pk").as[String].getOrElse("")}",
         |  "w": "${reqBody.downField("w").as[String].getOrElse("")}",
         |  "n": "${reqBody.downField("n").as[String].getOrElse("")}",
         |  "d": ${reqBody.downField("d").as[BigInt].getOrElse("")}e0
         |}
         |""".stripMargin

    Http(s"$connection${request.uri}").headers(reqHeaders).postData(body).asBytes
  }

  def getBoxBytes(boxId: String): Response = {
    Http(s"$connection/utxo/byIdBinary/$boxId").asBytes
  }

  /**
   * Send generate transaction request to the node
   *
   * @param address   destination address for transaction
   * @param value     value of transaction
   * @param inputsRaw input boxes for transaction
   * @return response from the node
   */
  def generateTransaction(address: String, value: Long, inputsRaw: Vector[Box] = null): Response = {
    val transactionGenerateBody: String =
      s"""
         |{
         |  "requests": [
         |    {
         |      "address": "$address",
         |      "value": $value
         |    }
         |  ],
         |  "fee": $transactionFee,
         |  "inputsRaw": [${if (inputsRaw != null) inputsRaw.map(f => s""""${f.boxBytes(this)}"""").mkString(",") else ""}]
         |}
         |""".stripMargin
    Http(s"$connection/wallet/transaction/generate").headers(authHeader).postData(transactionGenerateBody).asBytes
  }

  /**
   * Send candidateWithTxs request to the node
   *
   * @param transactions for the request
   * @return response from the node
   */
  def candidateWithTxs(transactions: Vector[Transaction]): Response = {
    Logger.debug(
      s"""
         |List of transactions for candidateWithTxs:
         |$transactions
         |""".stripMargin)
    val candidateWithTxsBody: String =
      s"""
         |[
         |  ${transactions.map(tx => s"""${tx.details.noSpaces}""").mkString(",")}
         |]
         |""".stripMargin
    Http(s"$connection/mining/candidateWithTxs").headers(authHeader).postData(candidateWithTxsBody).asBytes
  }

  /**
   * Fetch unspent boxes of wallet
   *
   * @param minHeight minimum height of boxes to get
   * @param minConfirmation minimum confirmation num of boxes to get
   * @return list of unspent boxes
   */
  def fetchUnspentBoxes(minHeight: Int, minConfirmation: Int): Response = {
    Http(s"$connection/wallet/boxes/unspent?minConfirmations=$minConfirmation&minInclusionHeight=$minHeight")
      .headers(authHeader).asBytes
  }

  /**
   * Fetch all boxes of wallet
   *
   * @param minHeight minimum height of boxes to get
   * @param minConfirmation minimum confirmation num of boxes to get
   * @return list of all boxes
   */
  def fetchBoxes(minHeight: Int, minConfirmation: Int): Response = {
    Http(s"$connection/wallet/boxes?minConfirmations=$minConfirmation&minInclusionHeight=$minHeight")
      .headers(authHeader).asBytes
  }

  /**
   * Get address of the script
   *
   * @param script the script
   * @return address corresponding to the script
   */
  def p2sAddress(script: String): Response = {
    Http(s"$connection/script/p2sAddress")
      .headers(authHeader)
      .postData(script)
      .asBytes
  }

  /**
   * Check if a box exists in the wallet
   *
   * @param boxId id of the box
   * @return true if exists, false o.w.
   */
  def isBoxExists(boxId: String): Boolean = {
    val response = Http(s"$connection/utxo/byId/$boxId").headers(defaultHeader).asBytes
    if (response.isSuccess) true else false
  }
}

final case class NodeClientError(message: String) extends Throwable(message)