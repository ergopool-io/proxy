package pool

import helpers.{ConfigTrait, Helper}
import io.circe.Json
import loggers.Logger
import scalaj.http.{Http, HttpResponse}

import scala.math.BigDecimal

trait PoolConfig extends ConfigTrait {
  val connection: String
  def poolConnection: String = connection

  var walletAddress: String = _
  var difficultyFactor: BigDecimal = _
  var transactionRequestsValue: Long = _
  var maxChunkSize: Short

  // Routes
  val poolServerValidationRoute: String
  val poolServerConfigRoute: String = readKey("pool.route.config")
  def poolServerSpecificConfigRoute(pk: String): String = readKey("pool.route.specific_config")
    .replaceFirst("<pk>", pk)

  override def toString: String = {
    s"""
      |{
      |   "connection": "$connection",
      |   "config": {
      |     "wallet": "$walletAddress",
      |     "difficulty_factor": $difficultyFactor,
      |     "transaction_request_value": $transactionRequestsValue,
      |     "max_chunk_size": $maxChunkSize
      |   }
      |}
      |""".stripMargin
  }

  /**
   * Load specific config using the pk
   *
   * @param pk key of the node
   * @return true if operation was a success
   */
  def loadConfig(pk: String): Boolean = {
    val response = fetchConfig(poolServerSpecificConfigRoute(pk))
    if (response.isSuccess) {
      val poolConfig: Json = Helper.ArrayByte(response.body).toJson
      val cursor = poolConfig.hcursor

      walletAddress = cursor.downField("wallet_address").as[String].getOrElse(null)
      difficultyFactor = BigDecimal(cursor.downField("pool_base_factor").as[Double].getOrElse(0.0))
      transactionRequestsValue = cursor.downField("reward").as[Long].getOrElse(0)
      maxChunkSize = cursor.downField("max_chunk_size").as[Short].getOrElse(0)
      true
    }
    else {
      Logger.error(s"$connection${poolServerSpecificConfigRoute(pk)}")
      Logger.error(Helper.ArrayByte(response.body).toString)
      false
    }
  }

  // $COVERAGE-OFF$
  /**
   * Get config from the pool server
   *
   * @return [[Json]] the config from the pool server
   */
  def fetchConfig(route: String): HttpResponse[Array[Byte]] = {
    Http(s"$connection$route").asBytes
  }
  // $COVERAGE-ON$
}