package proxy

import helpers.Helper
import io.circe.HCursor
import play.api.mvc.{RawBuffer, Request}
import proxy.loggers.Logger
import proxy.node.Node
import proxy.status.ProxyStatus

object ProxyService {
  /**
   * create share body from cursor
   * @param cursor cursor to json body
   * @return
   */
  private def shareBody(cursor: HCursor): String = {
    s"""
       |{
       |  "pk": "${cursor.downField("pk").as[String].getOrElse("")}",
       |  "w": "${cursor.downField("w").as[String].getOrElse("")}",
       |  "nonce": "${cursor.downField("n").as[String].getOrElse("")}",
       |  "d": "${cursor.downField("d").as[BigInt].getOrElse("")}"
       |}
       |""".stripMargin
  }

  /**
   * Create share request body from miner request
   *
   * @param request [[Request]] The request to get body info
   * @return [[Iterable]]
   */
  def getShareRequestBody(request: Request[RawBuffer]): Iterable[String] = {
    val reqBody: HCursor = Helper.RawBufferValue(request.body).toJson.hcursor
    val shares = reqBody.values

    if (shares.isEmpty) {
      Iterable[String](shareBody(reqBody))
    }
    else {
      shares.get.map(item => shareBody(item.hcursor))
    }
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
