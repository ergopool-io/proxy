package proxy.node

import helpers.Helper
import io.circe.Json
import proxy.loggers.Logger
import proxy.status.ProxyStatus
import proxy.{Config, PoolShareQueue, Response}

import scala.math.BigDecimal

class MiningCandidate(response: Response) {
  private var resp: Response = response
  private val responseBody: Json = Helper.ArrayByte(response.body).toJson

  /**
   * Get response for mining/candidate from the node response and handle proof
   * @return
   */
  def getResponse: String = {
    Node.checkRemainBoxesTransaction()
    try {
      checkHeader()
    }
    catch {
      case error: ProxyStatus.PoolRequestException =>
        Logger.error(s"MiningCandidate - ${error.getMessage}")
    }
    miningCandidateBody()
  }

  /**
   * Clean /mining/candidate response and put pb in it
   *
   * @return [[String]]
   */
  private def miningCandidateBody(): String = {
    val cursor = Helper.ArrayByte(resp.body).toJson.hcursor
    val b: BigDecimal = cursor.downField("b").as[BigDecimal].getOrElse(BigDecimal("0"))
    s"""
       |{
       |  "msg": "${cursor.downField("msg").as[String].getOrElse("")}",
       |  "b": $b,
       |  "pk": "${cursor.downField("pk").as[String].getOrElse("")}",
       |  "pb": ${(b * Config.poolDifficultyFactor).toBigInt}
       |}
       |""".stripMargin
  }

  /**
   * Check if msg is changed
   * If true, then send proof to the pool server
   */
  private def checkHeader(): Unit = {
    val cursor = responseBody.hcursor

    val header = cursor.downField("msg").as[String].getOrElse({
      throw new Throwable("Can not read Key = \"msg\"")
    })
    if (header != Config.blockHeader) {
      pushProof()
      Config.blockHeader = header
    }
  }

  /**
   * Get or create the proof and push it to queue
   *
   * @return [[Proof]] the proof
   */
  private def pushProof(): Unit = {
    val cursor = responseBody.hcursor
    val proof = Proof(cursor.downField("proof").as[Json].getOrElse(Json.Null))

    if (proof != null) {
      PoolShareQueue.push(null, proof)
    }
    else {
      try {
        Config.genTransactionInProcess = true
        val candidate = Node.createProof()
        if (candidate != null) this.resp = candidate

      }
      catch {
        case error: Throwable =>
          throw error
      }
      finally {
        Config.genTransactionInProcess = false
      }
    }
  }
}
