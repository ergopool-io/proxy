import javax.inject.Singleton
import play.api.http.HttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._
import loggers.Logger

import scala.concurrent._

@Singleton
class ErrorHandler extends HttpErrorHandler {
  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)(
        s"""
           |{
           |   "error": 500,
           |   "reason": "Client Error",
           |   "detail": "$message"
           |}
           |""".stripMargin).as("application/json")
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Logger.error(s"Exception happened - ${request.method} ${request.uri}", exception)
    Future.successful(
      InternalServerError(
        s"""
           |{
           |   "error": 500,
           |   "reason": "Internal Server Error",
           |   "detail": "${exception.getMessage}"
           |}
           |""".stripMargin).as("application/json")
    )
  }
}
