package proxy.status

/**
 * Contains proxy status
 */
object ProxyStatus {
  private val health = new Health
  private var _reason: String = "Starting Proxy"
  private var _category: String = "Proxy"

  /**
   * Set status of proxy
   *
   * @param health [[Short]] status of proxy
   * @param category [[String]] category of reason
   * @param reason [[String]] reason for this status if not green
   */
  def setStatus(health: Short, category: String = "", reason: String = ""): Unit = {
    this.health.set(health)
    this._category = category
    this._reason = reason
  }

  def reset(): Boolean = {
    if (category != "Config") {
      this.setStatus(StatusType.green)
      true
    }
    else {
      false
    }
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

  /**
   * Getter for category
   * @return
   */
  def category: String = this._category

  override def toString: String = {
    s"""
       |{
       |   "health": "${this.health}"
       |   ${if (!this.isHealthy) ",\"reason\": \"[" + this._category + "] " + this._reason + "\"" else ""}
       |}
       |""".stripMargin
  }

  /**
   * Exception for events that mining should be disabled
   *
   * @param message [[String]] reason of being disabled
   */
  final class MiningDisabledException(message: String) extends Throwable("Mining has been disabled") {
    ProxyStatus.setStatus(StatusType.red, "Mining", message)
  }

  /**
   * Exception for events that proxy should be working but health is not green
   *
   * @param message [[String]] reason of being not healthy
   */
  final class PoolRequestException(message: String) extends Throwable {
    ProxyStatus.setStatus(StatusType.yellow, "Pool", message)
  }
}
