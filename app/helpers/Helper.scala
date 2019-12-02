package helpers

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import com.typesafe.config.Config
import io.circe.Json

object Helper {
  private val defaultConfig: Config = ConfigFactory.load("application.conf")
  /**
   * Read config from the config param if there's the key, else get it from the global config
   * @param config [[Configuration]] Config that the value should be read from it
   * @param key [[String]] Key to search for in the config
   * @return [[String]] The value of the key in config
   */
  def readConfig(config: Configuration, key: String): String = {
    config.getOptional[String](key).getOrElse(this.defaultConfig.getString(key))
  }

  def convertBodyToJson(body: Array[Byte]): Json = {
    io.circe.parser.parse(body.map(_.toChar).mkString).getOrElse(Json.Null)
  }
}
