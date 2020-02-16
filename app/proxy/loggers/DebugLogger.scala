package proxy.loggers

import proxy.Config

object DebugLogger {
  // The logger object
  private val logger = play.api.Logger("proxyDebugLogger")
  private val enabled: Boolean = Config.debug

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   */
  def error(message: => String): Unit = {
    if (this.enabled) logger.error(message)
  }

  /**
   * Logs a message with the `ERROR` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def error(message: => String, error: => Throwable): Unit = {
    if (this.enabled) logger.error(message, error)
  }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   */
  def debug(message: => String): Unit = {
    if (this.enabled) logger.warn(message)
  }

  /**
   * Logs a message with the `DEBUG` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def debug(message: => String, error: => Throwable): Unit = {
    if (this.enabled) logger.warn(message, error)
  }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   */
  def info(message: => String): Unit = {
    if (this.enabled) logger.info(message)
  }

  /**
   * Logs a message with the `INFO` level.
   *
   * @param message the message to log
   * @param error the associated exception
   */
  def info(message: => String, error: => Throwable): Unit = {
    if (this.enabled) logger.info(message, error)
  }
}
