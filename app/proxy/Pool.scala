package proxy

import helpers.Helper
import io.circe.Json
import play.api.mvc.{RawBuffer, Request}
import proxy.loggers.Logger
import proxy.node.Node
import proxy.status.{ProxyStatus, StatusType}
import scalaj.http.{Http, HttpResponse}

object Pool {
  /**
   * Send generated transaction to the pool server
   * @param pk [[String]] pk of miner
   * @param transaction [[String]] the generated transaction
   * @return [[HttpResponse]]
   */
  def sendTransaction(pk: String, transaction: String): HttpResponse[Array[Byte]] = {
    val generatedTransactionResponseBody: String =
      s"""
         |{
         |   "pk": "$pk",
         |   "transaction": $transaction
         |}
         |""".stripMargin

    // Send generated transaction to the pool server
    PoolShareQueue.lock()
    PoolShareQueue.waitUntilEmptied()
    Http(s"${Config.poolConnection}${Config.poolServerTransactionRoute}").headers(Seq(("Content-Type", "application/json"))).postData(generatedTransactionResponseBody).asBytes
  }

  /**
   * Send the proof to the pool server
   *
   * @return [[HttpResponse]]
   */
  def sendProof(): HttpResponse[Array[Byte]] = {
    PoolShareQueue.lock()

    PoolShareQueue.waitUntilEmptied()

    try {
      Config.lastPoolProofWasSuccess = false
      val response = Http(s"${Config.poolConnection}${Config.poolServerProofRoute}").headers(Seq(("Content-Type", "application/json"))).postData(Node.proof).asBytes
      Config.lastPoolProofWasSuccess = true
      response
    }
    catch {
      case error: Throwable =>
        throw new ProxyStatus.PoolRequestException(s"Exception happened when tried to send proof to pool: ${error.getMessage}")
    }
    finally {
      PoolShareQueue.unlock()
    }
  }

  /**
   * Send solution to the pool server
   *
   * @param request [[Request]] the request that contains solution
   */
  def sendSolution(request: Request[RawBuffer]): Unit = {
    val requestBody: Json = Helper.RawBufferValue(request.body).toJson
    val cursor = requestBody.hcursor
    try {
      val bodyForPool: String =
        s"""
           |{
           |  "pk": "${cursor.downField("pk").as[String].getOrElse("")}",
           |  "w": "${cursor.downField("w").as[String].getOrElse("")}",
           |  "nonce": "${cursor.downField("n").as[String].getOrElse("")}",
           |  "d": "${cursor.downField("d").as[BigInt].getOrElse("")}"
           |}
           |""".stripMargin
      PoolShareQueue.push(bodyForPool)
    } catch {
      case error: Throwable =>
        Logger.error(error.getMessage)
    }
  }

  /**
   * Get config from the pool server
   *
   * @return [[Json]] the config from the pool server
   */
  def config(): Json = {
    ProxyStatus.setStatus(StatusType.red, "Config", "Loading Pool Server Config")
    while (true) {
      try {
        val response = Http(s"${Config.poolConnection}${Config.poolServerConfigRoute}").asBytes
        ProxyStatus.setStatus(StatusType.green)
        return Helper.ArrayByte(response.body).toJson
      } catch {
        case error: Throwable =>
          Logger.error(error.getMessage)
          ProxyStatus.setStatus(StatusType.red, "Config", s"Error getting config from the pool: ${error.getMessage}")
          Thread.sleep(5000)
      }
    }
    Json.Null //dummy return for compilation
  }
}
