package loggers

import play.api.mvc._
import play.api.Logger
import scalaj.http.HttpResponse
import com.typesafe.config.ConfigFactory
import io.circe.syntax._
import io.circe.parser.parse
import io.circe.Json

class ServerLogger {
  // The logger object
  val logger = Logger("proxy")

  // Config for logging bodies that are not in Json format
  private val logNotJsonBody: Boolean = if (ConfigFactory.load().getString("play.logger.only_json") == "false") true else false

  /**
   * Log an http request
   * 
   * @param request [[Request[AnyContent]]] The request that should be logged
   */ 
  def logRequest(request: Request[AnyContent]): Unit = {

    // Remove body or convert it to String if it's not in Json format
    val body: Json = {
      try {
        parse(request.body.toString).getOrElse(Json.Null)
      } catch {
        case _: Throwable =>
          if (this.logNotJsonBody) {
            parse(request.body.toString).getOrElse(Json.Null)
          }
          else {
            "<Body is removed due to config>".asJson
          }
      }
    }
    
    val json = Json.obj(
      "method"  -> request.method.asJson,
      "path"    -> request.uri.asJson,
      "body"    -> body,
      "headers" -> request.headers.toMap.asJson,
    )
    this.logger.info(s"${json.noSpaces}")
  }

  /**
   * Log an http response
   * 
   * @param response [[HttpResponse[Array[Byte]] The response that should be logged
   */ 
  def logResponse(response: HttpResponse[Array[Byte]]): Unit = {
    
    // Remove body or convert it to String if it's not in Json format
    val body: Json = {
      try {
        parse(response.body.map(_.toChar).mkString).getOrElse(Json.Null)
      } catch {
        case _: Throwable =>
          if (this.logNotJsonBody) {
            parse(response.body.toString).getOrElse(Json.Null)
          }
          else {
            "<Body is removed due to config>".asJson
          }
      }
    }
    val json = Json.obj(
      "body"    -> body,
      "headers" -> response.headers.toMap.asJson,
    )
    this.logger.info(s"${json.noSpaces}")
  }
}
