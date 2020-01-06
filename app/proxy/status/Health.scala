package proxy.status

class Health {
  private var health: Short = StatusType.red

  override def toString: String = {
    health match {
      case StatusType.green => "GREEN"

      case StatusType.red => "RED"

      case StatusType.yellow => "YELLOW"
    }
  }

  /**
   * Set the status
   * @param status [[Short]] status to be set
   */
  def set(status: Short): Unit = {
    this.health = status
  }

  /**
   * Check if status is green
   *
   * @return
   */
  def isHealthy: Boolean = this.health == StatusType.green

  /**
   * Check if status is not red
   *
   * @return
   */
  def isWorking: Boolean = this.health != StatusType.red
}
