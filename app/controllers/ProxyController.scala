package controllers

import loggers.ServerLogger
import helpers.Helper
import javax.inject._
import play.api.mvc._
import play.api.Configuration
import play.api.http.HttpEntity
import scalaj.http.{Http, HttpResponse}
import akka.util.ByteString
import io.circe.{ACursor, HCursor, Json}
import play.api.libs.json.JsValue

/**
 * Proxy pass controller
 * 
 * @constructor Create new controller for proxy passing
 * @param cc Controller component
 * @param config Configuration object
 */
@Singleton
class ProxyController @Inject()(cc: ControllerComponents)(config: Configuration)(logger: ServerLogger) extends AbstractController(cc) {
  // The current block header
  private[this] var _blockHeader: String = ""
  def blockHeader: String = _blockHeader

  // Set the node params
  private[this] val nodeConnection: String = Helper.readConfig(config,"node.connection")

  // The proof for the node
  private[this] var theProof: String = ""

  // True if the last proof had been sent to the pool successfully, otherwise false
  private[this] var lastPoolProofWasSuccess: Boolean = true

  // Set pool server params
  private[this] val poolConnection: String = Helper.readConfig(config, "pool.connection")
  private[this] val poolDifficulty: BigInt = BigInt(Helper.readConfig(config,"pool.difficulty"))
  private[this] val poolServerSolutionRoute: String = Helper.readConfig(config,"pool.route.solution")
  private[this] val poolServerProofRoute: String = Helper.readConfig(config,"pool.route.proof")
  private[this] val poolServerGeneratedTransactionRoute: String = Helper.readConfig(config,"pool.route.new_transaction")

  // Wallet address
  private[this] val walletAddress: String = Helper.readConfig(config, "pool.wallet")

  // Api Key
  private[this] val apiKey: String = Helper.readConfig(config, "node.api_key")

  private[this] val transactionRequestsValue = Helper.readConfig(config, "pool.transaction.value")
  
  /**
   * A structure for responses from the node
   * 
   * @constructor Create a new object containing the params
   * @param statusCode [[Int]] The status code of the response
   * @param headers [[Map[String, String]]] The headers of the response
   * @param body [[Array[Byte]]] The body of the response
   * @param contentType [[String]] The content type of the response
   */
  case class ProxyResponse(statusCode: Int, headers: Map[String, String],var body: Array[Byte], contentType: String)

  /**
   * Send a request to a url with its all headers and body
   * 
   * @param url [[String]] Servers url
   * @param request [[Request[AnyContent]]] The request to send
   * @return [[HttpResponse]] Response from the server
   */ 
  private def sendRequest(url: String, request: Request[AnyContent]): HttpResponse[Array[Byte]] = {
    
    // Prepare the request headers
    val reqHeaders: Seq[(String, String)] = request.headers.headers

    // Send the incoming request to node
    val response: HttpResponse[Array[Byte]] = {
      if (request.method == "GET") {
        Http(url).headers(reqHeaders).asBytes
      }
      else {
        request.body match {
          case t: AnyContentAsJson =>
            Http(url).headers(reqHeaders).postData(play.api.libs.json.Json.toBytes(request.body.asJson.get)).asBytes
          case _ =>
            // Prepare the request body
            val body: String = request.body.toString

            Http(url).headers(reqHeaders).postData(body).asBytes
        }
      }
    }
    response
  }

  /**
   * Prepare and return the response with its all headers and body
   *
   * @param response [[HttpResponse]] The request to send
   * @return [[ProxyResponse]] Prepared response
   */
  private def sendResponse(response: HttpResponse[Array[Byte]]): ProxyResponse = {
    
    // Convert the headers to Map[String, String] type
    val respHeaders: Map[String, String] = response.headers.map({
      case (key, value) => 
        key -> value.mkString(" ")
    })

    // Remove the ignored headers
    val contentType: String = respHeaders.getOrElse("Content-Type", "")
    val filteredHeaders: Map[String, String] = respHeaders.removed("Content-Type").removed("Content-Length")

    // Return the response
    ProxyResponse(
      statusCode = response.code, 
      headers = filteredHeaders,
      body = response.body,
      contentType = contentType
    )
  }

  /**
   * Update global proof
   * @param proof [[Json]] The json that contains the proof
   * @return [[Unit]]
   */
  private def updateProof(proof: Json): Unit = {
    val proofVal = proof.hcursor.downField("proof").as[Json].getOrElse(Json.Null)
    this.theProof = {
      if (proofVal != Json.Null) {
        val cursor: HCursor = proof.hcursor
        val proofCursor: ACursor = cursor.downField("proof")
        //        println(proofCursor.downField("txProofs").downArray.downField("levels").as[Json].getOrElse(Json.Null).toString())
        val txProof: ACursor = proofCursor.downField("txProofs").downArray

        s"""
           |{
           |    "pk": "${cursor.downField("pk").as[String].getOrElse("")}",
           |    "msg_pre_image": "${proofCursor.downField("msgPreimage").as[String].getOrElse("")}",
           |    "leaf": "${txProof.downField("leaf").as[String].getOrElse("")}",
           |    "levels": ${txProof.downField("levels").as[Json].getOrElse(Json.Null)}
           |}
           |""".stripMargin
      }
      else
        ""
    }
  }

  /**
   * Generate transaction and make a new proof
   * @return [[Json]]
   */
  def createProof(): Json = {
    try {
      val reqHeaders: Seq[(String, String)] = Seq(("api_key", this.apiKey), ("Content-Type", "application/json"))
      val transactionGenerateBody: String =
        s"""
           |{
           |  "requests": [
           |    {
           |      "address": "${this.walletAddress}",
           |      "value": ${this.transactionRequestsValue}
           |    }
           |  ],
           |  "fee": 1000000,
           |  "inputsRaw": []
           |}
           |""".stripMargin
      val generatedTransaction: HttpResponse[Array[Byte]] = Http(s"${this.nodeConnection}/wallet/transaction/generate").headers(reqHeaders).postData(transactionGenerateBody).asBytes
      val generatedTransactionResponseBody: String = generatedTransaction.body.map(_.toChar).mkString


      // Send generated transaction to the pool server
      Http(s"${this.poolConnection}${this.poolServerGeneratedTransactionRoute}").headers(Seq(("Content-Type", "application/json"))).postData(generatedTransactionResponseBody).asBytes

      val candidateWithTxsBody: String =
        s"""
           |[
           |  {
           |    "transaction": ${generatedTransactionResponseBody},
           |    "cost": ${this.transactionRequestsValue + 1000000}
           |  }
           |]
           |""".stripMargin
      val candidateWithTxs: HttpResponse[Array[Byte]] = Http(s"${this.nodeConnection}/mining/candidateWithTxs").headers(reqHeaders).postData(candidateWithTxsBody).asBytes

      val response: Json = Helper.convertBodyToJson(candidateWithTxs.body)

      // Update the proof
      updateProof(response)

      response
    } catch {
      case _: Throwable =>
        Json.Null
    }
  }

  /**
   * Send node proof to the pool server
   * @return [[Unit]]
   */
  def sendProofToPool(): Unit = {
    if (this.theProof != "") {
      try {
        this.lastPoolProofWasSuccess = false
        Http(s"${this.poolConnection}${this.poolServerProofRoute}").headers(Seq(("Content-Type", "application/json"))).postData(this.theProof).asBytes
        this.lastPoolProofWasSuccess = true
      } catch {
        case _: Throwable =>
      }
    }
  }

  /**
   * Returns the response of the clients requests from the node
   *
   * @param request [[Request[AnyContent]]] The request to send
   * @return [[ProxyResponse]] Response from the node
   */
  private def proxy(request: Request[AnyContent], config: Configuration = this.config, connection: String = this.nodeConnection): ProxyResponse = {

    // Log the request
    logger.logRequest(request)

    // Send the request to the node
    val response: HttpResponse[Array[Byte]] = this.sendRequest(s"${connection}${request.uri}", request)

    // Log the response
    logger.logResponse(response)

    // Return the response
    this.sendResponse(response)
  }

  /**
   * Action handler for proxy passing
   *
   * @return [[Result]] Response from the node
   */
  def proxyPass(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>

    // Send the request to node and get its response
    val response: ProxyResponse = this.proxy(request)

    Result(
      header = ResponseHeader(response.statusCode, response.headers),
      body = HttpEntity.Strict(ByteString(response.body), Some(response.contentType))
    )
  }

  /**
   * Action handler to send solution to node and resend it to the pool server if it's a correct solution
   *
   * @return [[Result]] Response from the node
   */
  def solution(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (!this.lastPoolProofWasSuccess) sendProofToPool()

    // Prepare the request headers
    val reqHeaders: Seq[(String, String)] = request.headers.headers
    val reqBody: JsValue = request.body.asJson.get
    val body: String =
      s"""
        |{
        |  "pk": "${(reqBody \ ("pk")).as[String]}",
        |  "w": "${(reqBody \ ("w")).as[String]}",
        |  "n": "${(reqBody \ ("n")).as[String]}",
        |  "d": ${(reqBody \ ("d")).as[BigInt]}e0
        |}
        |""".stripMargin
    logger.logRequest(request)

    val rawResponse: HttpResponse[Array[Byte]] = Http(s"${this.nodeConnection}${request.uri}").headers(reqHeaders).postData(body).asBytes

    logger.logResponse(rawResponse)

    // Send the request to node and get its response
    val response: ProxyResponse = this.sendResponse(rawResponse)

    // Send the request to the pool server if the nodes response is 200
    if (response.statusCode == 200) {
      try {
        Http(s"${this.poolConnection}${this.poolServerSolutionRoute}").headers(reqHeaders).postData(body).asBytes
      } catch {
        case _: Throwable =>
      }
    }

    Result(
      header = ResponseHeader(response.statusCode, response.headers),
      body = HttpEntity.Strict(ByteString(response.body), Some(response.contentType))
    )
  }

  /**
   * Clean /mining/candidate response and put pb in it
   * @param cursor [[HCursor]] a cursor to json for navigating in the body
   * @return [[String]]
   */
  def miningCandidateBody(cursor: HCursor): String = {
    s"""
       |{
       |  "msg": "${cursor.downField("msg").as[String].getOrElse("")}",
       |  "b": ${cursor.downField("b").as[BigInt].getOrElse(BigInt("0"))},
       |  "pk": "${cursor.downField("pk").as[String].getOrElse("")}",
       |  "pb": ${this.poolDifficulty}
       |}
       |""".stripMargin
  }

  /**
   * Action handler to put "pb" in the body of response for route /mining/candidate
   *
   * @return [[Result]] Response from the node with the key "pb"
   */
  def getMiningCandidate: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>

    // Log the request
    logger.logRequest(request)

    // Send the request to the node
    val response: HttpResponse[Array[Byte]] = this.sendRequest(s"${this.nodeConnection}${request.uri}", request)

    // Get the response in ProxyResponse format
    val preparedResponse: ProxyResponse = this.sendResponse(response)

    val newResponse: HttpResponse[Array[Byte]] = {
      if (preparedResponse.statusCode == 200) {
        // Get the pool difficulty from the config and put it in the body
        val body: Json = Helper.convertBodyToJson(response.body)
        val cursor: HCursor = body.hcursor

        // Send block header to pool server if it's new
        val responseBlockHeader: String = cursor.downField("msg").as[String].getOrElse("")
        val clearedBody: String = {
          if (responseBlockHeader != this.blockHeader) {
            // Change block header
            this._blockHeader = responseBlockHeader

            // Get new body or set old body if an error occurred
            val newBodyWithProof: Json = {
              updateProof(body)
              if (this.theProof == "") {
                val respBody: Json = createProof()
                sendProofToPool() // TODO: make it async
                if (respBody != Json.Null) respBody else body
              }
              else {
                sendProofToPool()
                body
              }
            }
            miningCandidateBody(newBodyWithProof.hcursor)
          }
          else {
            miningCandidateBody(cursor)
          }
        }

        preparedResponse.body = clearedBody.getBytes
        new HttpResponse[Array[Byte]](clearedBody.getBytes, response.code, response.headers)
      }
      else {
        response
      }
    }

    // Log the response
    logger.logResponse(newResponse)

    Result(
      header = ResponseHeader(preparedResponse.statusCode, preparedResponse.headers),
      body = HttpEntity.Strict(ByteString(preparedResponse.body), Some(preparedResponse.contentType))
    )
  }

  /**
   * Action handler for sending share to the pool server
   *
   * @return [[Result]] Response from the pool server
   */
  def sendShare: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    if (!this.lastPoolProofWasSuccess) sendProofToPool()

    try {
      // Prepare the request headers
      val reqHeaders: Seq[(String, String)] = request.headers.headers
      val reqBody: JsValue = request.body.asJson.get
      val body: String =
        s"""
           |{
           |  "pk": "${(reqBody \ ("pk")).as[String]}",
           |  "w": "${(reqBody \ ("w")).as[String]}",
           |  "n": "${(reqBody \ ("n")).as[String]}",
           |  "d": ${(reqBody \ ("d")).as[BigInt]}e0
           |}
           |""".stripMargin

      val rawResponse: HttpResponse[Array[Byte]] = Http(s"${this.poolConnection}${this.poolServerSolutionRoute}").headers(reqHeaders).postData(body).asBytes

      // Send the request to pool server and get its response
      val response: ProxyResponse = this.sendResponse(rawResponse)

      Result(
        header = ResponseHeader(response.statusCode, response.headers),
        body = HttpEntity.Strict(ByteString(response.body), Some(response.contentType))
      )
    } catch {
      case _: Throwable =>
        Ok(Json.obj().toString()).as("application/json")
    }
  }
}
