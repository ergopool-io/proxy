package proxy.action

import akka.util.ByteString
import play.api.http.HttpEntity
import play.api.mvc._
import proxy.PoolShareQueue
import proxy.node.Node
import proxy.status.{ProxyStatus, StatusType}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Action handler to handle mining routes
 * Check if proxy is working
 * check if pool is unlock
 */
case class MiningAction[A](action: Action[A]) extends Action[A] with play.api.Logging {
  private def errorResponse(message: String): Future[Result] = Future.successful(
    Result(
      header = ResponseHeader(500),
      body = HttpEntity.Strict(
        ByteString(
          s"""
             |{
             |   "error": 500,
             |   "reason": "Internal Server Error",
             |   "detail": "$message"
             |}
             |""".stripMargin
        ),
        Some("application/json")
      )
    )
  )

  def apply(request: Request[A]): Future[Result] = {
    if (!ProxyStatus.isWorking) {
      if (ProxyStatus.category == "Mining - TxsGen") {
        if (Node.gapTransaction.isMined) {
          Node.removeUnprotectedSpentBoxes()
          Node.addBoxes(Node.gapTransaction)
          ProxyStatus.setStatus(StatusType.green, "Mining - TxsGen")
          return action(request)
        }
      }
      this.errorResponse(ProxyStatus.reason)
    }
    else if (PoolShareQueue.isLock) {
      this.errorResponse("Transaction is being created")
    }
    else {
      action(request)
    }
  }

  override def parser: BodyParser[A] = action.parser

  override def executionContext: ExecutionContext = action.executionContext
}