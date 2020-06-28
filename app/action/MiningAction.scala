package action

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.mvc._
import proxy.Proxy

import scala.concurrent.{ExecutionContext, Future}

/**
 * Action handler to handle mining routes
 * Check if proxy is working
 * check if pool is unlock
 */
case class MiningAction[A](proxy: Proxy)(action: Action[A]) extends Action[A] with play.api.Logging {
  def apply(request: Request[A]): Future[Result] = {
    if (!proxy.status.isWorking) {
      Future.successful(
        Result(
          header = ResponseHeader(500),
          body = HttpEntity.Strict(
            ByteString(
              s"""
                 |{
                 |   "error": 500,
                 |   "reason": "Internal Server Error",
                 |   "detail": "${proxy.status}"
                 |}
                 |""".stripMargin
            ),
            Some("application/json")
          )
        )
      )
    }
    else {
      action(request)
    }
  }

  override def parser: BodyParser[A] = action.parser
  override def executionContext: ExecutionContext = action.executionContext
}