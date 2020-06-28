package helpers

import com.typesafe.config.ConfigFactory
import play.api.Configuration

trait ConfigTrait {
  val config = Configuration(ConfigFactory.load())

  /**
   * Read the config and return the value of the key
   *
   * @param key key to find
   * @param default default value if the key is not found
   * @return value of the key
   */
  def readKey(key: String, default: String = null): String = {
    config.getOptional[String](key).getOrElse(default)
  }
}
