package loggers

import play.api.mvc._
import play.api.Logger
import akka.util.ByteString
import scalaj.http.HttpResponse
import play.api.libs.json.{JsValue, Json, JsObject, JsNull}

class ServerLogger {
  private val logger = Logger("proxy")

  def logRequest(request : Request[AnyContent]): Unit = {
    val body : JsValue = request.body.asJson.getOrElse(JsNull)
    
    var json = Json.toJson(Map(
      "method"  -> Json.toJson(request.method),
      "path"    -> Json.toJson(request.uri),
      "body"    -> body,
      "headers" -> Json.toJson(request.headers.toMap),
    ))
    this.logger.info(s"${json.toString}")
  }

  def logResponse(response : HttpResponse[Array[Byte]]): Unit = {
    var json = Json.toJson(Map(
      "body"    -> Json.parse(response.body),
      "headers" -> Json.toJson(response.headers.toMap),
    ))
    this.logger.info(s"${json.toString}")
  }
}
