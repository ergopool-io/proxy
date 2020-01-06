package proxy.status

/**
 * Contains proxy status
 */
object ProxyStatus {
  private val health = new Health
  private var _reason: String = "Starting Proxy"

  /**
   * Set status of proxy
   *
   * @param health [[Short]] status of proxy
   * @param reason [[String]] reason for this status if not green
   */
  def setStatus(health: Short, reason: String = ""): Unit = {
    this.health.set(health)
    this._reason = reason
  }

  /**
   * Check if proxy is healthy
   *
   * @return [[Boolean]] true if is not red
   */
  def isHealthy: Boolean = this.health.isHealthy

  /**
   * Check if proxy is working
   *
   * @return
   */
  def isWorking: Boolean = this.health.isWorking

  /**
   * Getter for reason
   * @return
   */
  def reason: String = this._reason

  override def toString: String = {
    s"""
       |{
       |   "health": "${this.health}"
       |   ${if (!this.isHealthy) ",\"reason\": \"" + this._reason + "\"" else ""}
       |}
       |""".stripMargin
  }

  /**
   * Exception for events that mining should be disabled
   *
   * @param message [[String]] reason of being disabled
   */
  final class MiningDisabledException(message: String) extends Throwable("Mining has been disabled") {
    ProxyStatus.setStatus(StatusType.red, message)
  }

  /**
   * Exception for events that proxy should be working but health is not green
   *
   * @param message [[String]] reason of being not healthy
   */
  final class PoolRequestException(message: String) extends Throwable {
    ProxyStatus.setStatus(StatusType.yellow, message)
  }
}
