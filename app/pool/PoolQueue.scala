package pool

import helpers.Helper
import io.circe.Json
import io.circe.syntax._
import loggers.Logger
import node.{Proof, Share, Transaction}
import scalaj.http.{Http, HttpResponse}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.Breaks._

/**
 * Queue for sending pool request asynchronously
 * It tries to send each request in the queue until it's empty
 */
trait PoolQueue {
  val connection: String
  var maxChunkSize: Short
  val poolServerValidationRoute: String
  def packetBody(json: Json): String

  type PoolPacket = Either[Share, (Transaction, Proof)]
  type PoolShare = Right[Share, (Transaction, Proof)]
  type PoolTxP = Left[Share, (Transaction, Proof)]

  var queue: mutable.Queue[PoolPacket] = mutable.Queue.empty
  var isRunning: Boolean = false
  var transaction: Transaction = _
  var proof: Proof = _
  val clientErrorMaxTry: Short = 4

  /**
   * Run queue if it's not already running
   */
  def runQueue(): Unit = {
    if (!isRunning) {
      isRunning = true
      callFutureRun()
    }
  }

  def run(exitIfEmpty: Boolean = false): Unit = {
    var clientErrorCount: Int = 0
    var lastFailedChunkLength: Int = 0
    var work: Boolean = true
    while (work) {
      if (queue.nonEmpty) {
        breakable {
          try {
            if (isTxsP(queue.head)) {
              Logger.debug(
                s"""
                   |Pool Queue found a TxsP:
                   |${queue.head}
                   |""".stripMargin)
              val t = queue.dequeue().asInstanceOf[PoolShare].value
              transaction = t._1
              proof = t._2
            }
            if (transaction == null || proof == null) {
              Logger.debug(
                s"""
                   |null Tx/P -> clearing queue:
                   |tx: $transaction
                   |proof: $proof
                   |----------------------------------
                   |""".stripMargin)
              queue = queue.dropWhile(f => isShare(f))
              break
            }

            val items = queue
              .take(maxChunkSize)
              .takeWhile(p => isShare(p))
              .map(f => f.left.getOrElse(null).body)

            if (items.nonEmpty) { // In case of queue has been cleared between "while condition" and "take action"
              val response: HttpResponse[Array[Byte]] = sendPacket(items, proof, transaction)

              // Pop request if it's accepted or rejected
              if (response.isSuccess) {
                popNItem(items.length)
                clientErrorCount = 0
                lastFailedChunkLength = 0
              }
              else if (response.isClientError) {
                Logger.debug(s"Client error from the pool: \n${Helper.ArrayByte(response.body).toString}")

                if (items.length > lastFailedChunkLength) {
                  clientErrorCount = 0
                  lastFailedChunkLength = items.length
                }

                if (clientErrorCount >= clientErrorMaxTry) {
                  Logger.debug("Reach maximum try - clearing chunk")
                  popNItem(items.length)
                  clientErrorCount = 0
                  lastFailedChunkLength = 0
                }
                else
                  clientErrorCount += 1
              } else {
                Logger.debug(s"Internal error from the pool: \n${Helper.ArrayByte(response.body).toString}")
                Thread.sleep(5000)
              }
            }
          }
          catch {
            case _: scala.util.control.ControlThrowable =>
              break
            case e: Throwable =>
              Logger.error("Error occurred when tried to send request to pool", e)
          }
        }
      } else if (exitIfEmpty) work = false
    }

    Logger.error("run stopped")
  }

  // $COVERAGE-OFF$
  /**
   * Tries to send requests to the pool until the queue is empty
   * It is working asynchronously
   */
  def callFutureRun(): Unit = {
    Future {
      run()
    }
  }
  // $COVERAGE-ON$

  private def popNItem(n: Int): Unit = {
    (1 to n).foreach(_ => pop())
  }

  // $COVERAGE-OFF$
  /**
   * Removes first element in the queue
   * Unlocks the queue if the element was a lock
   */
  private def pop(): Unit = {
    try {
      queue.dequeue()
    } catch {
      case _: java.util.NoSuchElementException => // In case of queue cleared before popping
      case _: java.lang.IndexOutOfBoundsException => // In case of queue cleared before popping
    }
  }

  /**
   * Sends requests to the pool server
   *
   * @param onGoingQueue [[mutable.Queue]] request to send
   * @return response from the pool
   */
  def sendPacket(onGoingQueue: mutable.Queue[Json], proof: Proof, transaction: Transaction): HttpResponse[Array[Byte]] = {
    val body = packetBody(Helper.convertToJson(
      s"""
        |{
        |   "transaction": ${transaction.details},
        |   "proof": ${proof.body},
        |   "shares": ${onGoingQueue.asJson}
        |}
        |""".stripMargin))
    Logger.info(s"sending shares to pool, # of shares: ${onGoingQueue.size}")
    Http(s"$connection$poolServerValidationRoute")
      .header("Content-Type", "application/json")
      .postData(body).asBytes
  }
  // $COVERAGE-ON$

  /**
   * Is element a (Transaction, Proof) tuple
   *
   * @param elem element to check
   * @return true if is a (Transaction, Proof) tuple
   */
  private def isTxsP(elem: PoolPacket): Boolean = elem.isInstanceOf[PoolShare]

  /**
   * Add shares to the queue
   *
   * @param shares [[Iterable]] iterable of shares
   */
  def push(shares: Share*): Unit = {
    queue.enqueue(shares.map(f => Left(f)): _*)
    runQueue()
  }

  /**
   * Add transaction and proof to the queue
   *
   * @param txs   [[Transaction]] transaction to add
   * @param proof [[Proof]] proof to add
   */
  def push(txs: Transaction, proof: Proof): Unit = {
    Logger.debug(
      s"""
         |New Transaction & Proof pushed:
         |$txs
         |$proof
         |""".stripMargin)
    queue.enqueue(Right((txs, proof)))
  }

  private def isShare(elem: PoolPacket): Boolean = elem.isInstanceOf[PoolTxP]
}