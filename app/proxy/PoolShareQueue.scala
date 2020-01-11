package proxy

import helpers.{Helper, List}
import io.circe.Json
import proxy.loggers.Logger
import scalaj.http.{Http, HttpResponse}
import io.circe.syntax._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Queue for sending pool request asynchronously
 * It tries to send each request in the queue until it's empty
 */
class PoolShareQueue {
  private val queue: mutable.Queue[Json] = mutable.Queue.empty
  private var _lock: Boolean = false
  private var isRunning: Boolean = false

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
   * Sends requests to the pool server
   *
   * @param onGoingQueue [[mutable.Queue]] request to send
   * @return response from the pool
   */
  private def send(onGoingQueue: mutable.Queue[Json]): HttpResponse[Array[Byte]] = {
    Http(s"${Config.poolConnection}${Config.poolServerSolutionRoute}").header("Content-Type", "application/json").postData(onGoingQueue.asJson.toString()).asBytes
  }

  /**
   * Tries to send requests to the pool until the queue is empty
   * It is working asynchronously
   */
  private def run(): Unit = {
    Future {
      this.isRunning = true
      while (queue.nonEmpty) {
        try {
          val items = queue.take(Config.maxChunkSize)

          if (items.nonEmpty) { // In case of queue was cleared between while condition and take action
            val response: HttpResponse[Array[Byte]] = send(items)

            // Pop request if it's accepted or rejected
            if (response.isSuccess || response.isClientError) this.popNItem(items.length)
            else Thread.sleep(5000)
          }
        }
        catch {
          case e: Throwable =>
            Logger.error("Error occurred when tried to send request to pool", e)
        }
      }
      this.isRunning = false
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
   * @param share [[String]] body of request
   */
  def push(share: String): Unit = {
    this.cls.queue.enqueue(Helper.convertToJson(share))
    runQueue()
  }

  /**
   * Add shares to the queue
   *
   * @param shares [[Iterable]] iterable of shares
   */
  def push(shares: Iterable[String]): Unit = {
    this.cls.queue.enqueueAll(shares.map(s => Helper.convertToJson(s)))
    runQueue()
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
   * Return size of queue
   * @return [[Int]]
   */
  def length: Int = {
    this.cls.queue.length
  }

  /**
   * Reset the queue: clear queue and unlock
   */
  def resetQueue(): Unit = {
    this.cls.queue.clear()
    this.cls.unlock()
  }

  /**
   * Check if queue is not empty
   * @return [[Boolean]]
   */
  def isNonEmpty: Boolean = {
    this.cls.queue.nonEmpty
  }

  /**
   * Wait until queue is empty
   * sleep for 0.5s if is not
   */
  def waitUntilEmptied(): Unit = {
    while (this.isNonEmpty) Thread.sleep(500)
  }
}