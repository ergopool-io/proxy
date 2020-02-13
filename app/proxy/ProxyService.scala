package proxy

import helpers.Helper
import io.circe.HCursor
import play.api.mvc.{RawBuffer, Request}
import proxy.node.{Node, Share}
import proxy.status.ProxyStatus

object ProxyService {
  /**
   * Create share request body from miner request
   *
   * @param request [[Request]] The request to get body info
   * @return [[Iterable]]
   */
  def getShareRequestBody(request: Request[RawBuffer]): List[Share] = {
    val reqBody: HCursor = Helper.RawBufferValue(request.body).toJson.hcursor
    val shares = reqBody.values

    if (shares.isEmpty) {
      List[Share](Share(reqBody))
    }
    else {
      shares.get.toList.map(item => Share(item.hcursor))
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
       |       "connection": "${Config.poolConnection}",
       |       "config": {
       |         "wallet": "${Config.walletAddress}",
       |         "difficulty_factor": ${Config.poolDifficultyFactor},
       |         "max_chunk_size": ${Config.maxChunkSize},
       |         "transaction_request_value": ${Config.transactionRequestsValue}
       |       },
       |       "node": {
       |          "pk": "${Node.pk}"
       |       }
       |     },
       |     "status": $ProxyStatus
       |   }
       |}
       |""".stripMargin
  }
}
