package proxy.status

import io.circe.{Encoder, Json}
import io.circe.syntax._

/**
 * Contains proxy status
 */
class ProxyStatus {
  private val RED = "RED"
  private val YELLOW = "YELLOW"
  private val GREEN = "GREEN"
  private val stats = Map[String, Status](
    ("config", new Status(RED, "Error getting config from the pool")),
    ("mnemonic", new Status(RED, "Load mnemonic to continue or remove the file")),
    ("protectedTx", new Status(YELLOW, "Protected transaction is null")),
    ("poolTx", new Status(YELLOW, "Pool transaction is null")),
    ("activeSyncing", new Status(YELLOW, "Loading blocks and boxes from the node")),
    ("walletLock", new Status(RED, "Wallet is lock")),
    ("nodeError", new Status(RED, "Error from the node"))
  )
  val config: Status        = stats("config")
  val mnemonic: Status      = stats("mnemonic")
  val protectedTx: Status   = stats("protectedTx")
  val poolTx: Status        = stats("poolTx")
  val activeSyncing: Status = stats("activeSyncing")
  val walletLock: Status    = stats("walletLock")
  val nodeError: Status     = stats("nodeError")

  /**
   * Reset the status
   *
   * @return true if operation was a success
   */
  def reset(): Unit = {
    stats.foreach(_._2.setHealthy())
  }

  /**
   * Check if proxy is healthy
   *
   * @return [[Boolean]] true if is not red
   */
  def isHealthy: Boolean = {
    stats.forall(_._2.isHealthy)
  }

  /**
   * Check if proxy is working
   *
   * @return
   */
  def isWorking: Boolean = {
    stats.filter(_._2.color == RED).forall(_._2.isHealthy)
  }

  def status: String = {
    var isRed = false
    var isYellow = false
    stats.foreach(s => {
      if (!s._2.isHealthy) {
        if (s._2.color == RED)
          isRed = true
        else
          isYellow = true
      }
    })
    if (isRed) RED
    else if (isYellow) YELLOW
    else GREEN
  }

  override def toString: String = {
    implicit val encodeFoo: Encoder[Map[String, Status]] = (a: Map[String, Status]) => Json.obj(
      a.map(s => (s._1, Json.fromString(s._2.toString))).toVector: _*
    )
    val health = status
    s"""
       |{
       |   "health": "$health"
       |   ${if (health != GREEN) ",\"reason\": \"" + stats.filter(s => s._2.color == health && !s._2.isHealthy).toList.map(item => item._2).mkString(",") + "\"" else ""}
       |}
       |""".stripMargin
  }
}
