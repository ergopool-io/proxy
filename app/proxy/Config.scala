package proxy

import com.typesafe.config.ConfigFactory
import helpers.Helper
import io.circe.Json
import org.ergoplatform.appkit.NetworkType
import play.api.Configuration
import proxy.node.Node

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.BigDecimal

/**
 * Global object for configuration
 */
object Config {
  // Default configuration for application
  val config = Configuration(ConfigFactory.load())
  // Addresses to use in protection script
  lazy val minerAddress: String = Node.deriveKey(Helper.readConfig(config, "node.address.miner"))
  lazy val withdrawAddress: String = Node.deriveKey(Helper.readConfig(config, "node.address.withdraw"))
  val playSecret: String = Helper.readConfig(config, "play.http.secret.key")
  // Api Key
  val apiKey: String = Helper.readConfig(config, "node.api_key")
  val transactionFee: Int = 1000000
  // Set the node params
  val nodeConnection: String = Helper.readConfig(config, "node.connection")
  // The pool server connection
  val poolConnection: String = Helper.readConfig(config, "pool.connection")
  val mnemonicFilename: String = Helper.readConfig(config, "mnemonic.filename", "mnemonic")
  // The pool server routes
  val poolServerValidationRoute: String = Helper.readConfig(config, "pool.route.share")
  val poolServerConfigRoute: String = Helper.readConfig(config, "pool.route.config")
  var networkType: NetworkType = NetworkType.MAINNET
  // Check if a transaction generation is in process
  var genTransactionInProcess = false
  // Pool config
  var walletAddress: String = ""
  var poolDifficultyFactor: BigDecimal = BigDecimal(0.0)
  var transactionRequestsValue: Long = 0
  var maxChunkSize: Short = 0
  // The current block header
  var blockHeader: String = ""
  // The proof for the node
  var theProof: String = ""
  // True if the last proof had been sent to the pool successfully, otherwise false
  var lastPoolProofWasSuccess: Boolean = true
  var poolServerSpecificConfigRoute: String = Helper.readConfig(config, "pool.route.specific_config")

  val debug: Boolean = Helper.readConfig(config, "debug", "false") == "true"

  def lockAddress: String = Mnemonic.address.toString

  def loadPoolConfig(): Unit = {
    // Get pool configs
    Future {
      val poolConfig: Json = Pool.specificConfig()
      val cursor = poolConfig.hcursor

      this.walletAddress = cursor.downField("wallet_address").as[String].getOrElse("")
      this.poolDifficultyFactor = BigDecimal(cursor.downField("pool_base_factor").as[Double].getOrElse(0.0))
      this.transactionRequestsValue = cursor.downField("reward").as[Long].getOrElse(0)
      this.maxChunkSize = cursor.downField("max_chunk_size").as[Short].getOrElse(0)
    }
  }
}