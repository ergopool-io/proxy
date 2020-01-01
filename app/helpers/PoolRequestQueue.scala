package helpers

import loggers.ServerLogger
import scalaj.http.{Http, HttpResponse}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Request class
 * @param url [[String]] route to send the request to
 * @param headers [[Seq[(String, String)]]] body of request
 * @param body [[String]] body of request
 */
private case class Request(url: String, headers: Seq[(String, String)], body: String)

/**
 * Queue for sending pool request asynchronously
 * It tries to send each request in the queue until it's empty
 */
class PoolRequestQueue {

  private val queue: mutable.Queue[Request] = mutable.Queue.empty
  private var _lock: Boolean = false
  private val logger: ServerLogger = new ServerLogger
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
    }
  }

  /**
   * Sends requests to the pool server
   * @param request [[Request]] request to send
   * @return response from the pool
   */
  private def send(request: Request): HttpResponse[Array[Byte]] = {
    Http(s"${request.url}").headers(request.headers).postData(request.body).asBytes
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
          val request: Request = queue.head

          val response: HttpResponse[Array[Byte]] = send(request)

          // Pop request if it's accepted or rejected
          if (response.isSuccess || response.isClientError) this.pop()
        }
        catch {
          case e: Throwable =>
            logger.logger.error("Error occurred when tried to send request to pool", e)
        }
      }
      this.isRunning = false
    }
  }
}

object PoolRequestQueue {
  var cls = new PoolRequestQueue

  /**
   * Add request to the queue
   * @param url [[String]] route to send the request to
   * @param body [[String]] body of request
   */
  def push(url: String, headers: Seq[(String, String)], body: String): Unit = {
    this.cls.queue.enqueue(Request(url, headers, body))
    if (!this.cls.isRunning) this.cls.run()
  }

  /**
   * Check if queue is locked
   * @return [[Boolean]]
   */
  def isLock: Boolean = this.cls._lock

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
   * Check if the route parameter request is in the queue
   * @param route [[String]] The route to check
   * @return [[Boolean]]
   */
  def isInQueue(route: String): Boolean = {
    this.cls.queue.exists(p => p.url.endsWith(route))
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
}