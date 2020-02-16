package proxy

import helpers.Helper
import helpers.Helper.ArrayByte
import io.circe.Json
import play.api.mvc.{RawBuffer, Request}
import proxy.loggers.Logger
import proxy.node.{Node, Share}
import proxy.status.{ProxyStatus, StatusType}
import scalaj.http.Http

object Pool {

  /**
   * Send solution to the pool server
   *
   * @param request [[Request]] the request that contains solution
   */
  def sendSolution(request: Request[RawBuffer]): Unit = {
    val requestBody: Json = Helper.RawBufferValue(request.body).toJson
    val cursor = requestBody.hcursor
    try {
      PoolShareQueue.push(Share(cursor))
    } catch {
      case error: Throwable =>
        Logger.error(error.toString)
    }
  }

  /**
   * Get config from the pool server
   *
   * @return [[Json]] the config from the pool server
   */
  def specificConfig(): Json = {
    while (true) {
      try {
        val miningCandidate = Http(s"${Config.nodeConnection}/mining/candidate").asBytes
        val body = ArrayByte(miningCandidate.body).toJson.hcursor
        Node.pk = body.downField("pk").as[String].getOrElse("")
        Config.poolServerSpecificConfigRoute = Config.poolServerSpecificConfigRoute.replaceFirst("<pk>", s"${Node.pk}")

        return config(Config.poolServerSpecificConfigRoute)
      } catch {
        case error: Throwable =>
          Logger.error(s"Node: ${error.toString}")
          ProxyStatus.setStatus(StatusType.red, "Config", s"Error getting pk from the node: ${error.toString}")
          Thread.sleep(5000)
      }
    }
    Json.Null // dummy return for compilation
  }

  /**
   * Get config from the pool server
   *
   * @return [[Json]] the config from the pool server
   */
  def config(route: String = Config.poolServerConfigRoute): Json = {
    while (true) {
      try {
        val response = Http(s"${Config.poolConnection}$route").asBytes
        if (response.isSuccess) {
          ProxyStatus.setStatus(StatusType.green, "Config")
          return Helper.ArrayByte(response.body).toJson
        }
      } catch {
        case error: Throwable =>
          Logger.error(s"Pool: ${error.toString}")
          ProxyStatus.setStatus(StatusType.red, "Config", s"Error getting config from the pool: ${error.toString}")
          Thread.sleep(5000)
      }
    }
    Json.Null //dummy return for compilation
  }
}
