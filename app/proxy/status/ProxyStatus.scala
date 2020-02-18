package proxy.status

/**
 * Contains proxy status
 */
object ProxyStatus {
  private val health = new Health
  private var _reason: String = "Starting Proxy"
  private var _category: String = "Proxy"

  /**
   * Reset proxy status
   *
   * @return
   */
  def reset(): Unit = {
    this.health.set(StatusType.green)
    this._category = ""
    this._reason = ""
  }

  /**
   * Check if proxy is working
   *
   * @return true if health is not red
   */
  def isWorking: Boolean = this.health.isWorking

  /**
   * Getter for reason
   *
   * @return
   */
  def reason: String = this._reason

  /**
   * Getter for category
   *
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

  def disableMining(message: String, subCategory: String = null): Unit = {
    val category: String = if (subCategory != null) s"Mining - $subCategory" else "Mining"
    ProxyStatus.setStatus(StatusType.red, category, message)
  }

  /**
   * Set status of proxy
   *
   * @param health   [[Short]] status of proxy
   * @param category [[String]] category of reason
   * @param reason   [[String]] reason for this status if not green
   */
  def setStatus(health: Short, category: String = "", reason: String = ""): Unit = {
    if (this.isWorking || this._category == category) {
      this.health.set(health)
      this._category = category
      this._reason = reason
    }
  }

  /**
   * Check if proxy is healthy
   *
   * @return [[Boolean]] true if is green
   */
  def isHealthy: Boolean = this.health.isHealthy

  /**
   * Exception for events that mining should be disabled
   *
   * @param message [[String]] reason of being disabled
   */
  final class MiningDisabledException(message: String, subCategory: String = null) extends Throwable(s"Mining has been disabled - $message") {
    val category: String = if (subCategory != null) s"Mining - $subCategory" else "Mining"
    ProxyStatus.setStatus(StatusType.red, category, message)
  }

  /**
   * Exception for not enough boxes exception on transaction generate
   *
   * @param message [[String]] reason of being disabled
   */
  final class NotEnoughBoxesException(message: String) extends Throwable(s"Not enough boxes on transaction generate - $message") {
    ProxyStatus.setStatus(StatusType.yellow, "Mining - NotEnoughBoxes", message)
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
