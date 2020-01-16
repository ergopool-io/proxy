package proxy.node

import helpers.Helper
import io.circe.{ACursor, HCursor, Json}
import play.api.mvc.{RawBuffer, Request}
import proxy.{Config, Response}
import scalaj.http.{Http, HttpResponse}

object Node {
  private[this] var pk: String = ""
  private[this] var _proof: String = ""

  def proof: String = _proof

  /**
   * Update value of proof
   *
   * @param value [[String]] response of candidateWithTxs or miningCandidate with proof
   */
  def proof_=(value: String): Unit = {
    if (value != "") {
      val proofValue = Helper.convertToJson(value)
      val cursor: ACursor = proofValue.hcursor
      val txProof: ACursor = cursor.downField("txProofs").downArray

      _proof =
      s"""
         |{
         |    "pk": "${this.pk}",
         |    "msg_pre_image": "${cursor.downField("msgPreimage").as[String].getOrElse("")}",
         |    "leaf": "${txProof.downField("leaf").as[String].getOrElse("")}",
         |    "levels": ${txProof.downField("levels").as[Json].getOrElse(Json.Null)}
         |}
         |""".stripMargin
    }
  }

  /**
   * Send a request to a url with its all headers and body
   *
   * @param uri [[String]] Servers url
   * @param request [[Request[RawBuffer]]] The request to send
   * @return [[Response]] Response from the server
   */
  def sendRequest(uri: String, request: Request[RawBuffer]): Response = {
    // Prepare the request headers
    val reqHeaders: Seq[(String, String)] = request.headers.headers

    val response: HttpResponse[Array[Byte]] = {
      try {
        if (request.method == "GET") {
          Http(s"${Config.nodeConnection}$uri").headers(reqHeaders).asBytes
        }
        else {
          Http(s"${Config.nodeConnection}$uri").headers(reqHeaders).postData(Helper.RawBufferValue(request.body).toString).asBytes
        }
      }
      catch {
        case error: Throwable =>
          throw new Throwable(s"Node - $uri: ${error.getMessage}", error)
      }
    }

    // Convert the headers to Map[String, String] type
    val respHeaders: Map[String, String] = response.headers.map({
      case (key, value) =>
        key -> value.mkString(" ")
    })

    // Remove the ignored headers
    val contentType: String = respHeaders.getOrElse("Content-Type", "")
    val filteredHeaders: Map[String, String] = respHeaders.removed("Content-Type").removed("Content-Length")

    // Return the response
    Response(
      statusCode = response.code,
      headers = filteredHeaders,
      body = response.body,
      contentType = contentType
    )
  }

  /**
   * Send solution to the node
   *
   * @param request [[Request]] the request from the miner
   * @return [[Response]]
   */
  def sendSolution(request: Request[RawBuffer]): Response = {
    // Prepare the request headers
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

    val rawResponse: HttpResponse[Array[Byte]] = Http(s"${Config.nodeConnection}${request.uri}").headers(reqHeaders).postData(body).asBytes
    Response(rawResponse)
  }

  /**
   * Send generate transaction request to the node
   *
   * @return [[HttpResponse]]
   */
  def generateTransaction(): HttpResponse[Array[Byte]] = {
    val reqHeaders: Seq[(String, String)] = Seq(("api_key", Config.apiKey), ("Content-Type", "application/json"))
    val transactionGenerateBody: String =
      s"""
         |{
         |  "requests": [
         |    {
         |      "address": "${Config.walletAddress}",
         |      "value": ${Config.transactionRequestsValue}
         |    }
         |  ],
         |  "fee": 1000000,
         |  "inputsRaw": []
         |}
         |""".stripMargin
    Http(s"${Config.nodeConnection}/wallet/transaction/generate").headers(reqHeaders).postData(transactionGenerateBody).asBytes
  }

  /**
   * Send candidateWithTxs request to the node
   *
   * @param transaction [[String]] generated transaction
   * @return [[HttpResponse]]
   */
  def candidateWithTxs(transaction: String): HttpResponse[Array[Byte]] = {
    val reqHeaders: Seq[(String, String)] = Seq(("api_key", Config.apiKey), ("Content-Type", "application/json"))
    val candidateWithTxsBody: String =
      s"""
         |[
         |  {
         |    "transaction": $transaction,
         |    "cost": 50000
         |  }
         |]
         |""".stripMargin
    val response = Http(s"${Config.nodeConnection}/mining/candidateWithTxs").headers(reqHeaders).postData(candidateWithTxsBody).asBytes
    this.pk = Helper.ArrayByte(response.body).toJson.hcursor.downField("pk").as[String].getOrElse("")

    response
  }

  // $COVERAGE-OFF$
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
  // $COVERAGE-ON$
}
