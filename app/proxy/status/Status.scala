package proxy.status

class Status(statusColor: String, statusErrorMessage: String) {
  val color: String = statusColor
  val errorMessage: String = statusErrorMessage
  var isHealthy: Boolean = true
  var _reason: String = _

  /**
   * Set status to unhealthy
   *
   * @param reason reason of being unhealthy
   */
  def setUnhealthy(reason: String = null): Unit = {
    isHealthy = false
    _reason = reason
  }

  /**
   * Set status to healthy
   */
  def setHealthy(): Unit = {
    isHealthy = true
    _reason = null
  }

  override def toString: String = {
    if (isHealthy) "GREEN"
    else s"""$color - $errorMessage${if (_reason != null) s" - " + _reason else ""}"""
  }
}
