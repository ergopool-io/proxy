package proxy.loggers

import com.typesafe.config.ConfigFactory
import helpers.Helper
import io.circe.Json
import io.circe.syntax._
import io.circe.parser.parse
import play.api.mvc.{RawBuffer, Request}
import scalaj.http.HttpResponse

object Logger {
  // The logger object
  val logger = play.api.Logger("proxy")

  // Config for logging bodies that are not in Json format
  private val logNotJsonBody: Boolean = if (ConfigFactory.load().getString("play.logger.only_json") == "false") true else false

  /**
   * Log an http request
   *
   * @param request [[Request[AnyContent]]] The request that should be logged
   */
  def logRequest(request: Request[RawBuffer]): Unit = {
    // Remove body or convert it to String if it's not in Json format
    val body: Json = {
      try {
        Helper.ConvertRaw(request.body).toJson
      } catch {
        case _: Throwable =>
          if (this.logNotJsonBody) {
            parse(Helper.ConvertRaw(request.body).toString).getOrElse(Json.Null)
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
      "headers" -> response.headers.asJson,
    )
    this.logger.info(s"${json.noSpaces}")
  }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   */
  def error(message: => String): Unit = {
    logger.error(message)
  }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def error(message: => String, error: => Throwable): Unit = {
    logger.error(message, error)
  }
}
