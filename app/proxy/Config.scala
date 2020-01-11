package proxy

import com.typesafe.config.ConfigFactory
import helpers.Helper
import io.circe.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.BigDecimal
import play.api.Configuration

/**
 * Global object for configuration
 */
object Config {
  // Default configuration for application
  val config = Configuration(ConfigFactory.load())

  // Check if a transaction generation is in process
  var genTransactionInProcess = false

  // Api Key
  val apiKey: String = Helper.readConfig(config, "node.api_key")

  // Pool config
  var walletAddress: String = ""
  var poolDifficultyFactor: BigDecimal = BigDecimal(0.0)
  var transactionRequestsValue: Long = 0
  var maxChunkSize: Short = 0
  loadPoolConfig()

  // The current block header
  var blockHeader: String = ""

  // Set the node params
  val nodeConnection: String = Helper.readConfig(config,"node.connection")

  // The proof for the node
  var theProof: String = ""

  // True if the last proof had been sent to the pool successfully, otherwise false
  var lastPoolProofWasSuccess: Boolean = true

  // The pool server connection
  val poolConnection: String = Helper.readConfig(config, "pool.connection")

  // The pool server routes
  val poolServerSolutionRoute: String = Helper.readConfig(config,"pool.route.solution")
  val poolServerProofRoute: String = Helper.readConfig(config,"pool.route.proof")
  val poolServerConfigRoute: String = Helper.readConfig(config,"pool.route.config")
  val poolServerTransactionRoute: String = Helper.readConfig(config,"pool.route.new_transaction")

  def loadPoolConfig(): Unit = {
    // Get pool configs
    Future[Unit] {
      val poolConfig: Json = Pool.config()
      val cursor = poolConfig.hcursor

      this.walletAddress = cursor.downField("wallet_address").as[String].getOrElse("")
      this.poolDifficultyFactor = BigDecimal(cursor.downField("pool_base_factor").as[Double].getOrElse(0.0))
      this.transactionRequestsValue = cursor.downField("reward").as[Long].getOrElse(0)
      this.maxChunkSize = cursor.downField("max_chunk_size").as[Short].getOrElse(0)
    }
  }
}