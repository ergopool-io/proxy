package proxy.action

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.mvc._
import proxy.PoolShareQueue
import proxy.status.ProxyStatus

import scala.concurrent.{ExecutionContext, Future}

/**
 * Action handler to handle mining routes
 * Check if proxy is working
 * check if pool is unlock
 */
case class MiningAction[A](action: Action[A]) extends Action[A] with play.api.Logging {
  def apply(request: Request[A]): Future[Result] = {
    if (!ProxyStatus.isWorking) {
      Future.successful(
        Result(
          header = ResponseHeader(500),
          body = HttpEntity.Strict(
            ByteString(
              s"""
                 |{
                 |   "error": 500,
                 |   "reason": "Internal Server Error",
                 |   "detail": "${ProxyStatus.reason}"
                 |}
                 |""".stripMargin.map(_.toByte)
            ),
            Some("application/json")
          )
        )
      )
    }
    else if (PoolShareQueue.isLock) {
      Future.successful(
        Result(
          header = ResponseHeader(500),
          body = HttpEntity.Strict(
            ByteString(
              s"""
                 |{
                 |   "error": 500,
                 |   "reason": "Internal Server Error",
                 |   "detail": "Transaction is being created"
                 |}
                 |""".stripMargin.map(_.toByte)
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