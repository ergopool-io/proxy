package proxy

import helpers.Helper
import io.circe.Json
import io.circe.syntax._
import proxy.loggers.Logger
import proxy.node.{Node, Proof, Share, Transaction}
import scalaj.http.{Http, HttpResponse}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.Breaks._

/**
 * Queue for sending pool request asynchronously
 * It tries to send each request in the queue until it's empty
 */
class PoolShareQueue {
  private var queue: mutable.Queue[Either[Share, (Transaction, Proof)]] = mutable.Queue.empty
  private var _lock: Boolean = false
  private var isRunning: Boolean = false
  private var transaction: Transaction = _
  private var proof: Proof = _

  /**
   * Lock the queue
   */
  private def lock(): Unit = {
    this._lock = true
  }

  /**
   * Unlock the queue
   */
  private def unlock(): Unit = {
    this._lock = false
  }

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

  private def popNItem(n: Int): Unit = {
    (1 to n).foreach(_ => this.pop())
  }

  /**
   * Add element to the queue
   *
   * @param elem element to add to queue
   */
  private def push(elem: Either[Share, (Transaction, Proof)]): Unit = {
    this.queue.enqueue(elem)
  }

  /**
   * Add elements to the queue
   *
   * @param elems iterable of shares
   */
  private def push(elems: List[Either[Share, (Transaction, Proof)]]): Unit = {
    this.queue.enqueue(elems: _*)
  }

  /**
   * Sends requests to the pool server
   *
   * @param onGoingQueue [[mutable.Queue]] request to send
   * @return response from the pool
   */
  private def send(onGoingQueue: mutable.Queue[Json]): HttpResponse[Array[Byte]] = {
    val body =
      s"""
        |{
        |    "addresses": {
        |        "miner": "${Config.minerAddress}",
        |        "lock": "${Config.lockAddress}",
        |        "withdraw": "${Config.withdrawAddress}"
        |    },
        |    "pk": "${Node.pk}",
        |    "transaction": ${this.transaction.details},
        |    "proof": ${this.proof.body},
        |    "shares": ${onGoingQueue.asJson}
        |}
        |""".stripMargin
    Logger.debug(body)
    Http(s"${Config.poolConnection}${Config.poolServerValidationRoute}")
      .header("Content-Type", "application/json")
      .postData(body).asBytes
  }

  private def isTxsP(elem: Either[Share, (Transaction, Proof)]): Boolean = elem.isInstanceOf[Right[Share, (Transaction, Proof)]]
  private def isShare(elem: Either[Share, (Transaction, Proof)]): Boolean = elem.isInstanceOf[Left[Share, (Transaction, Proof)]]

  /**
   * Tries to send requests to the pool until the queue is empty
   * It is working asynchronously
   */
  private def run(): Unit = {
    Future {
      this.isRunning = true
      while (queue.nonEmpty) {
        breakable {
          try {
            if (isTxsP(queue.head)) {
              Logger.debug(
                s"""
                  |Pool Queue found a TxsP:
                  |${queue.head}
                  |""".stripMargin)
              val t = queue.dequeue().asInstanceOf[Right[Share, (Transaction, Proof)]].value
              if (t._1 != null) this.transaction = t._1
              this.proof = t._2
            }
            if (this.transaction == null || this.proof == null) {
              Logger.debug(
                s"""
                  |Empty transaction/proof:
                  |Transaction: ${this.transaction}
                  |Proof: ${this.proof}
                  |""".stripMargin)
              queue = queue.dropWhile(f => isShare(f))
              break
            }

            val items = queue
              .take(Config.maxChunkSize)
              .takeWhile(p => isShare(p))
              .map(f => f.left.getOrElse(null).asInstanceOf[Share].body)

            if (items.nonEmpty) { // In case of queue has been cleared between "while condition" and "take action"
              val response: HttpResponse[Array[Byte]] = send(items)

              // Pop request if it's accepted or rejected
              if (response.isSuccess) {
                this.popNItem(items.length)
              }
              else if (response.isClientError) {
                Logger.debug(s"Client error from the pool: \n${Helper.ArrayByte(response.body).toString}")
                queue = queue.dropWhile(f => isShare(f))
                this.transaction = null
                this.proof = null
              }
              else {
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
      }
      this.isRunning = false

      if (this.transaction == null && this.proof == null && queue.isEmpty) {
        Node.createProof()
      }
    }
  }
}

object PoolShareQueue {
  var cls = new PoolShareQueue

  /**
   * Run queue if it's not already running
   */
  private def runQueue(): Unit = {
    if (!this.cls.isRunning) this.cls.run()
  }

  /**
   * Add request to the queue
   *
   * @param share [[Share]] body of request
   */
  def push(share: Share): Unit = {
    Logger.debug(
      s"""
        |New Share pushed:
        |$share
        |""".stripMargin)
    this.cls.push(Left(share))
    runQueue()
  }

  /**
   * Add shares to the queue
   *
   * @param shares [[Iterable]] iterable of shares
   */
  def push(shares: List[Share]): Unit = {
    Logger.debug(
      s"""
        |New Shares pushed:
        |$shares
        |""".stripMargin)
    this.cls.push(shares.map(f => Left(f)))
    runQueue()
  }

  /**
   * Add transaction and proof to the queue
   *
   * @param txs [[Transaction]] transaction to add
   * @param proof [[Proof]] proof to add
   */
  def push(txs: Transaction, proof: Proof): Unit = {
    Logger.debug(
      s"""
        |New Transaction & Proof pushed:
        |$txs
        |$proof
        |""".stripMargin)
    this.cls.push(Right((txs, proof)))
  }

  /**
   * Check if queue is locked
   * @return [[Boolean]]
   */
  def isLock: Boolean = this.cls._lock

  /**
   * Lock the queue
   */
  def lock(): Unit = {
    this.cls.lock()
  }

  /**
   * Unlock the queue
   */
  def unlock(): Unit = {
    this.cls.unlock()
  }

  /**
   * Count of shares in the queue
   * @return
   */
  def sharesCount: Int = {
    this.cls.queue.count(p => this.cls.isShare(p))
  }

  /**
   * Reset the queue: clear queue and unlock
   */
  def resetQueue(): Unit = {
    this.cls.queue.clear()
    this.cls.unlock()
  }
}