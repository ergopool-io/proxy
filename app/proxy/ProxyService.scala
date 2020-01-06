package proxy

import helpers.Helper
import io.circe.HCursor
import play.api.mvc.{RawBuffer, Request}
import proxy.loggers.Logger
import proxy.node.Node
import proxy.status.ProxyStatus

object ProxyService {
  /**
   * Create share request body from miner request
   *
   * @param request [[Request]] The request to get body info
   * @return [[String]]
   */
  def getShareRequestBody(request: Request[RawBuffer]): String = {
    val reqBody: HCursor = Helper.ConvertRaw(request.body).toJson.hcursor
    s"""
       |{
       |  "pk": "${reqBody.downField("pk").as[String].getOrElse("")}",
       |  "w": "${reqBody.downField("w").as[String].getOrElse("")}",
       |  "nonce": "${reqBody.downField("n").as[String].getOrElse("")}",
       |  "d": "${reqBody.downField("d").as[BigInt].getOrElse("")}"
       |}
       |""".stripMargin
  }

  /**
   * Information of the proxy
   *
   * @return [[String]]
   */
  def proxyInfo: String = {
    s"""
       |{
       |   "proxy": {
       |     "pool": {
       |       "connection": "${Config.nodeConnection}",
       |       "config": {
       |         "wallet": "${Config.walletAddress}",
       |         "difficulty_factor": ${Config.poolDifficultyFactor},
       |         "transaction_request_value": ${Config.transactionRequestsValue}
       |       }
       |     },
       |     "status": $ProxyStatus
       |   }
       |}
       |""".stripMargin
  }

  /**
   * Send node proof to the pool server
   *
   * @return [[Unit]]
   */
  def sendProofToPool(): Unit = {
    if (Node.proof != "") {
      try {
        val proofValidation = Pool.sendProof()

        if (proofValidation.isClientError) {
          Node.proof = ""
        }
      }
      catch {
        case error: ProxyStatus.PoolRequestException =>
          Logger.error(error.getMessage)
      }
    }
  }
}
