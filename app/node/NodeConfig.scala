package node

import helpers.ConfigTrait

trait NodeConfig extends ConfigTrait {
  val connection: String = readKey("node.connection")

  val apiKey: String = readKey("node.api_key")

  val transactionFee: Int = 1000000
}
