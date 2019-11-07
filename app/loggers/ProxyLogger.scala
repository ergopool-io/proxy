package loggers

import play.api.mvc._
import play.api.Logger
import akka.util.ByteString
import scalaj.http.HttpResponse
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsValue, Json, JsObject, JsNull}

class ServerLogger {
  // The logger object
  private val logger = Logger("proxy")

  // Config for logging bodies that are not in Json format
  private val logNotJsonBody: Boolean = if (ConfigFactory.load().getString("log.not.json.body") == "true") true else false

  /**
   * Log an http request
   * 
   * @param request [[Request[AnyContent]]]
   */ 
  def logRequest(request: Request[AnyContent]): Unit = {

    // Remove body or convert it to String if it's not in Json format
    val body: JsValue = {
      try {
        request.body.asJson.getOrElse(JsNull)
      } catch {
        case _: Throwable => {
          if (this.logNotJsonBody) {
            Json.toJson(request.body.toString)
          }
          else {
            Json.toJson("<Body is removed due to config>")
          }
        }
      }
    }
    
    var json = Json.toJson(Map(
      "method"  -> Json.toJson(request.method),
      "path"    -> Json.toJson(request.uri),
      "body"    -> body,
      "headers" -> Json.toJson(request.headers.toMap),
    ))
    this.logger.info(s"${json.toString}")
  }

  /**
   * Log an http response
   * 
   * @param response [[HttpResponse[Array[Byte]]]]
   */ 
  def logResponse(response: HttpResponse[Array[Byte]]): Unit = {
    
    // Remove body or convert it to String if it's not in Json format
    val body: JsValue = {
      try {
        Json.parse(response.body)
      } catch {
        case _: Throwable => {
          if (this.logNotJsonBody) {
            Json.toJson(response.body.toString)
          }
          else {
            Json.toJson("<Body is removed due to config>")
          }
        }
      }
    }
    var json = Json.toJson(Map(
      "body"    -> body,
      "headers" -> Json.toJson(response.headers.toMap),
    ))
    this.logger.info(s"${json.toString}")
  }
}
