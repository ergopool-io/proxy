package controllers

import loggers.ServerLogger
import helpers.{Helper, PoolRequestQueue}
import javax.inject._
import play.api.mvc._
import play.api.Configuration
import play.api.http.HttpEntity
import scalaj.http.{Http, HttpResponse}
import akka.util.ByteString
import io.circe.{ACursor, HCursor, Json}
import io.swagger.v3.oas.models.media.{Content, MediaType, Schema}
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.oas.models.{OpenAPI, Operation, PathItem, Paths}
import io.swagger.v3.core.util.Yaml
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.BigDecimal

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

  // Check if a transaction generation is in process
  private[this] var genTransactionInProcess = false

  // True if the last proof had been sent to the pool successfully, otherwise false
  private[this] var lastPoolProofWasSuccess: Boolean = true

  // Set pool server params
  private[this] val poolConnection: String = Helper.readConfig(config, "pool.connection")
  private[this] val poolServerSolutionRoute: String = Helper.readConfig(config,"pool.route.solution")
  private[this] val poolServerProofRoute: String = Helper.readConfig(config,"pool.route.proof")
  private[this] val poolServerConfigRoute: String = Helper.readConfig(config,"pool.route.config")
  private[this] val poolServerGeneratedTransactionRoute: String = Helper.readConfig(config,"pool.route.new_transaction")

  // Api Key
  private[this] val apiKey: String = Helper.readConfig(config, "node.api_key")

  private[this] var miningDisabled: Boolean = false

  // Pool config
  private[this] var walletAddress: String = ""
  private[this] var poolDifficultyFactor: BigDecimal = BigDecimal(0.0)
  private[this] var transactionRequestsValue: Long = 0

  Future[Unit] {
    var poolConfig: HCursor = Json.Null.hcursor
    var poolConfigCompleted: Boolean = false
    while (!poolConfigCompleted) {
      val response: HCursor = {
        try {
          val response = Http(s"${this.poolConnection}${this.poolServerConfigRoute}").asBytes
          poolConfigCompleted = true
          ProxyStatus.setStatus(ProxyStatus.GREEN)
          Helper.convertBodyToJson(response.body).hcursor
        } catch {
          case error: Throwable =>
            logger.logger.error(error.getMessage)
            ProxyStatus.setStatus(ProxyStatus.RED, s"Error getting config from the pool: ${error.getMessage}")
            Thread.sleep(5000)
            Json.Null.hcursor
        }
      }
      poolConfig = response
    }
    this.walletAddress = poolConfig.downField("wallet_address").as[String].getOrElse("")
    this.poolDifficultyFactor = BigDecimal(poolConfig.downField("pool_difficulty_factor").as[Double].getOrElse(0.0))
    this.transactionRequestsValue = poolConfig.downField("reward").as[Long].getOrElse(0)
  }

  /**
   * A structure for responses from the node
   * 
   * @constructor Create a new object containing the params
   * @param statusCode [[Int]] The status code of the response
   * @param headers [[Map[String, String]]] The headers of the response
   * @param body [[Array[Byte]]] The body of the response
   * @param contentType [[String]] The content type of the response
   */
  case class ProxyResponse(statusCode: Int, headers: Map[String, String], var body: Array[Byte], contentType: String)

  /**
   * Exception for events that mining should be disabled
   * @param message [[String]] reason of being disabled
   */
  final class MiningDisabledException(message: String) extends Throwable("Mining has been disabled") {
    miningDisabled = true
    ProxyStatus.setStatus(ProxyStatus.RED, message)
  }

  /**
   * Contains proxy status
   */
  private object ProxyStatus {
    val GREEN: Boolean = true
    val RED: Boolean = false

    private var health: Boolean = RED
    private var reason: String = "Starting Proxy"

    def setStatus(health: Boolean, reason: String = ""): Unit = {
      this.health = health
      this.reason = reason
    }

    override def toString: String = {
      s"""
        |{
        |   "health": ${if (this.health) "\"GREEN\"" else "\"RED\","}
        |   ${if (!this.health) "\"reason\": \"" + this.reason + "\"" else ""}
        |}
        |""".stripMargin
    }
  }

  /**
   * Send a request to a url with its all headers and body
   * 
   * @param url [[String]] Servers url
   * @param request [[Request[RawBuffer]]] The request to send
   * @return [[HttpResponse]] Response from the server
   */ 
  private def sendRequest(url: String, request: Request[RawBuffer]): HttpResponse[Array[Byte]] = {
    // Prepare the request headers
    val reqHeaders: Seq[(String, String)] = request.headers.headers

    // Send the incoming request to node
    val response: HttpResponse[Array[Byte]] = {
      if (request.method == "GET") {
        Http(url).headers(reqHeaders).asBytes
      }
      else {
        Http(url).headers(reqHeaders).postData(Helper.ConvertRaw(request.body).toString).asBytes
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
  private def createProof(pk: String): Json = {
    this.genTransactionInProcess = true
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
      val transaction = generatedTransaction.body.map(_.toChar).mkString

      // Disable mining routes if transaction is not OK
      if (!generatedTransaction.isSuccess) {
        logger.logResponse(generatedTransaction)
        logger.logger.error(generatedTransaction.body.map(_.toChar).mkString)
        throw new MiningDisabledException(s"Route /wallet/transaction/generate failed with error code ${generatedTransaction.code}")
      }

      val generatedTransactionResponseBody: String =
        s"""
          |{
          |   "pk": "$pk",
          |   "transaction": $transaction
          |}
          |""".stripMargin

      // Send generated transaction to the pool server
      PoolRequestQueue.lock()
      while (PoolRequestQueue.isNonEmpty) Thread.sleep(500)
      val poolGeneratedTransactionResponse: HttpResponse[Array[Byte]] = Http(s"${this.poolConnection}${this.poolServerGeneratedTransactionRoute}").headers(Seq(("Content-Type", "application/json"))).postData(generatedTransactionResponseBody).asBytes

      if (poolGeneratedTransactionResponse.isSuccess) {
        val candidateWithTxsBody: String =
          s"""
             |[
             |  {
             |    "transaction": $transaction,
             |    "cost": 50000
             |  }
             |]
             |""".stripMargin
        val candidateWithTxs: HttpResponse[Array[Byte]] = Http(s"${this.nodeConnection}/mining/candidateWithTxs").headers(reqHeaders).postData(candidateWithTxsBody).asBytes
        val response: Json = Helper.convertBodyToJson(candidateWithTxs.body)

        // Update the proof
        this.updateProof(response)
        this.genTransactionInProcess = false

        response
      }
      else {
        Json.Null
      }
    } catch {
      case error: MiningDisabledException =>
        PoolRequestQueue.unlock()
        throw error
      case error: Throwable =>
        PoolRequestQueue.unlock()
        this.logger.logger.error(error.getMessage)
        this.genTransactionInProcess = false
        throw new MiningDisabledException(s"Creating proof failed: ${error.getMessage}")
    }
  }

  /**
   * Send node proof to the pool server
   * @return [[Unit]]
   */
  private def sendProofToPool(): Unit = {
    if (this.theProof != "") {
      PoolRequestQueue.lock()
      while (PoolRequestQueue.isNonEmpty) Thread.sleep(500)
      try {
        this.lastPoolProofWasSuccess = false
        Http(s"${this.poolConnection}${this.poolServerProofRoute}").headers(Seq(("Content-Type", "application/json"))).postData(this.theProof).asBytes
        this.lastPoolProofWasSuccess = true
      } catch {
        case error: Throwable =>
          this.logger.logger.error(error.getMessage)
      }
      PoolRequestQueue.unlock()
    }
  }

  /**
   * Returns the response of the clients requests from the node
   *
   * @param request [[Request[RawBuffer]]] The request to send
   * @return [[ProxyResponse]] Response from the node
   */
  private def proxy(request: Request[RawBuffer], config: Configuration = this.config): ProxyResponse = {

    // Log the request
//    logger.logRequest(request)

    // Send the request to the node
    val response: HttpResponse[Array[Byte]] = this.sendRequest(s"${this.nodeConnection}${request.uri}", request)

    // Log the response
//    logger.logResponse(response)

    // Return the response
    this.sendResponse(response)
  }

  /**
   * Action handler for proxy passing
   *
   * @return [[Result]] Response from the node
   */
  def proxyPass: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>

    // Send the request to node and get its response
    val response: ProxyResponse = this.proxy(request)

    val contentType = {
      if (response.contentType == "")
        "plain/text"
      else
        response.contentType
    }
    Result(
      header = ResponseHeader(response.statusCode, response.headers),
      body = HttpEntity.Strict(ByteString(response.body), Some(contentType))
    )
  }

  /**
   * Action handler to send solution to node and resend it to the pool server if it's a correct solution
   *
   * @return [[Result]] Response from the node
   */
  def sendSolution: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    if (!miningDisabled && !PoolRequestQueue.isLock) {
      if (!this.lastPoolProofWasSuccess) sendProofToPool()

      // Prepare the request headers
      val reqHeaders: Seq[(String, String)] = request.headers.headers
      val reqBody: HCursor = Helper.ConvertRaw(request.body).toJson.hcursor
      val body: String =
        s"""
           |{
           |  "pk": "${reqBody.downField("pk").as[String].getOrElse("")}",
           |  "w": "${reqBody.downField("w").as[String].getOrElse("")}",
           |  "n": "${reqBody.downField("n").as[String].getOrElse("")}",
           |  "d": ${reqBody.downField("d").as[BigInt].getOrElse("")}e0
           |}
           |""".stripMargin
      // logger.logRequest(request)

      val rawResponse: HttpResponse[Array[Byte]] = Http(s"${this.nodeConnection}${request.uri}").headers(reqHeaders).postData(body).asBytes

      // logger.logResponse(rawResponse)

      // Send the request to node and get its response
      val response: ProxyResponse = this.sendResponse(rawResponse)

      // Send the request to the pool server if the nodes response is 200
      if (response.statusCode == 200) {
        try {
          val bodyForPool: String =
            s"""
               |{
               |  "pk": "${reqBody.downField("pk").as[String].getOrElse("")}",
               |  "w": "${reqBody.downField("w").as[String].getOrElse("")}",
               |  "nonce": "${reqBody.downField("n").as[String].getOrElse("")}",
               |  "d": "${reqBody.downField("d").as[BigInt].getOrElse("")}"
               |}
               |""".stripMargin
          PoolRequestQueue.push(s"${this.poolConnection}${this.poolServerSolutionRoute}", reqHeaders, bodyForPool)
        } catch {
          case error: Throwable =>
            this.logger.logger.error(error.getMessage)
        }
      }

      Result(
        header = ResponseHeader(response.statusCode, response.headers),
        body = HttpEntity.Strict(ByteString(response.body), Some(response.contentType))
      )
    } else {
      Result(
        header = ResponseHeader(500),
        body = HttpEntity.Strict(ByteString(
          """
            |{
            |   "error": 500,
            |   "reason": "Internal Server Error",
            |   "detail": "Proxy status is RED"
            |}
            |""".stripMargin.map(_.toByte)), Some("application/json"))
      )
    }
  }

  /**
   * Clean /mining/candidate response and put pb in it
   * @param cursor [[HCursor]] a cursor to json for navigating in the body
   * @return [[String]]
   */
  private def miningCandidateBody(cursor: HCursor): String = {
    val b: BigDecimal = cursor.downField("b").as[BigDecimal].getOrElse(BigDecimal("0"))
    s"""
       |{
       |  "msg": "${cursor.downField("msg").as[String].getOrElse("")}",
       |  "b": $b,
       |  "pk": "${cursor.downField("pk").as[String].getOrElse("")}",
       |  "pb": ${(b * this.poolDifficultyFactor).toBigInt}
       |}
       |""".stripMargin
  }

  /**
   * Action handler to put "pb" in the body of response for route /mining/candidate
   *
   * @return [[Result]] Response from the node with the key "pb"
   */
  def getMiningCandidate: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    if (!miningDisabled && !PoolRequestQueue.isLock) {
      if (!this.genTransactionInProcess) {
        // Log the request
        // logger.logRequest(request)

        // Send the request to the node
        val response: HttpResponse[Array[Byte]] = this.sendRequest(s"${this.nodeConnection}${request.uri}", request)

        // Get the response in ProxyResponse format
        val preparedResponse: ProxyResponse = this.sendResponse(response)


        if (preparedResponse.statusCode == 200) {
          // Get the pool difficulty from the config and put it in the body
          val body: Json = Helper.convertBodyToJson(response.body)
          val cursor: HCursor = body.hcursor

          // Send block header to pool server if it's new
          val responseBlockHeader: String = cursor.downField("msg").as[String].getOrElse("")
          val clearedBody: String = {
            if (responseBlockHeader != this.blockHeader) {
              // Check if creating proof process was success
              var changeBlockHeader: Boolean = true
              // Get new body or set old body if an error occurred
              val newBodyWithProof: Json = {
                updateProof(body)
                if (this.theProof == "") {
                  val respBody: Json = createProof(cursor.downField("pk").as[String].getOrElse(""))
                  if (respBody != Json.Null) {
                    sendProofToPool()
                    respBody
                  }
                  else {
                    PoolRequestQueue.unlock()
                    changeBlockHeader = false
                    body
                  }
                }
                else {
                  sendProofToPool()
                  body
                }
              }
              // Change block header if tried to create proof and it was successful or proof was available
              if (changeBlockHeader)
                this._blockHeader = responseBlockHeader

              miningCandidateBody(newBodyWithProof.hcursor)
            }
            else {
              miningCandidateBody(cursor)
            }
          }

          preparedResponse.body = clearedBody.getBytes
        }

        Result(
          header = ResponseHeader(preparedResponse.statusCode, preparedResponse.headers),
          body = HttpEntity.Strict(ByteString(preparedResponse.body), Some(preparedResponse.contentType))
        )
      } else {
        InternalServerError
      }
    }
    else {
      Result(
        header = ResponseHeader(500),
        body = HttpEntity.Strict(ByteString(
          s"""
            |{
            |   "error": 500,
            |   "reason": "Internal Server Error",
            |   "detail": "${if (miningDisabled) "Proxy status is RED" else "Transaction is being created"}"
            |}
            |""".stripMargin.map(_.toByte)), Some("application/json"))
      )
    }
  }

  /**
   * Action handler for sending share to the pool server
   *
   * @return [[Result]] Response from the pool server
   */
  def sendShare: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    if (!miningDisabled && !PoolRequestQueue.isLock) {
      if (!this.lastPoolProofWasSuccess) sendProofToPool()

      try {
        // Prepare the request headers
        val reqHeaders: Seq[(String, String)] = request.headers.headers
        val reqBody: HCursor = Helper.ConvertRaw(request.body).toJson.hcursor
        val body: String =
          s"""
             |{
             |  "pk": "${reqBody.downField("pk").as[String].getOrElse("")}",
             |  "w": "${reqBody.downField("w").as[String].getOrElse("")}",
             |  "nonce": "${reqBody.downField("n").as[String].getOrElse("")}",
             |  "d": "${reqBody.downField("d").as[BigInt].getOrElse("")}"
             |}
             |""".stripMargin

        PoolRequestQueue.push(s"${this.poolConnection}${this.poolServerSolutionRoute}", reqHeaders, body)

        // Send the request to pool server and get its response
        Ok(Json.obj().toString()).as("application/json")
      } catch {
        case _: Throwable =>
          Ok(Json.obj().toString()).as("application/json")
      }
    } else {
      Result(
        header = ResponseHeader(500),
        body = HttpEntity.Strict(ByteString(
          """
            |{
            |   "error": 500,
            |   "reason": "Internal Server Error",
            |   "detail": ""
            |}
            |""".stripMargin.map(_.toByte)), Some("application/json"))
      )
    }
  }

  /**
   * Change swagger config to show pool routes
   * @return [[Result]] new swagger config
   */
  def changeSwagger: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    val openApi: OpenAPI = new OpenAPIV3Parser().read(s"${this.nodeConnection}/api-docs/swagger.conf")

    // Add pb in /mining/candidate
    val pbSchema = new Schema()
    pbSchema.setType("Integer")
    pbSchema.setExample(9876543210L)
    openApi.getComponents.getSchemas.get("ExternalCandidateBlock").addProperties("pb", pbSchema)

    // Add /mining/share

    val response200: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Share is valid")
      response
    }

    val response500: ApiResponse = {
      val response = new ApiResponse
      response.setDescription("Share is invalid")

      val schema: Schema[_] = new Schema()
      schema.set$ref("#/components/schemas/ApiError")

      val mediaType: MediaType = new MediaType
      mediaType.setSchema(schema)

      val content: Content = new Content
      content.addMediaType("application/json", mediaType)

      response.setContent(content)

      response
    }

    // Put 200 and 500 in responses
    val APIResponses: ApiResponses = {
      val responses = new ApiResponses

      responses.addApiResponse("200", response200)
      responses.addApiResponse("500", response500)

      response500.setDescription("Error")
      responses.setDefault(response500)

      responses
    }

    // Create a post operation
    val postOperation: Operation = {
      val security = new SecurityRequirement
      security.addList("ApiKeyAuth", "[api_key]")

      val op = new Operation
      op.addSecurityItem(security)
      op.setSummary("Submit share for current candidate")
      op.addTagsItem("mining")
      op.setRequestBody(openApi.getPaths.get("/mining/solution").getPost.getRequestBody)
      op.setResponses(APIResponses)

      op
    }

    // Add post Operation to paths
    val path: PathItem = new PathItem
    path.setPost(postOperation)

    // Reformat paths of openAPI
    val newPaths = new Paths
    openApi.getPaths.forEach({
      case (pathName, pathValue) =>
        newPaths.addPathItem(pathName, pathValue)
        if (pathName == "/mining/rewardAddress")
          newPaths.addPathItem("/mining/share", path)
    })
    openApi.setPaths(newPaths)

    val yaml = Yaml.pretty(openApi)

    Result(
      header = ResponseHeader(200),
      body = HttpEntity.Strict(ByteString(yaml), Some("application/json"))
    )
  }

  /**
   * Change /info to add proxy information
   * @return [[Result]] new info
   */
  def changeInfo: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    // Send the request to the node
    val response: HttpResponse[Array[Byte]] = this.sendRequest(s"${this.nodeConnection}${request.uri}", request)

    val respBody: Json = Helper.convertBodyToJson(response.body).deepMerge(io.circe.parser.parse(
      s"""
        |{
        |   "proxy": {
        |     "pool": {
        |       "connection": "${this.nodeConnection}",
        |       "config": {
        |         "wallet": "${this.walletAddress}",
        |         "difficulty_factor": ${this.poolDifficultyFactor},
        |         "transaction_request_value": ${this.transactionRequestsValue}
        |       }
        |     },
        |     "status": $ProxyStatus
        |   }
        |}
        |""".stripMargin).getOrElse(Json.Null))

    Result(
      header = ResponseHeader(200),
      body = HttpEntity.Strict(ByteString(respBody.toString()), Some("application/json"))
    )
  }
}
