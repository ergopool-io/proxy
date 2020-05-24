package proxy

import helpers.ConfigTrait

/**
 * Global object for configuration
 */
trait ProxyConfig extends ConfigTrait {
  val playSecret: String = readKey("play.http.secret.key")

  val withdraw: String = readKey("node.address.withdraw")

  val mnemonicFilename: String = readKey("mnemonic.filename", "mnemonic")
}
