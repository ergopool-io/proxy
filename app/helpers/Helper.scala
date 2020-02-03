package helpers

import akka.util.ByteString
import play.api.Configuration
import io.circe.Json
import javax.servlet.http.HttpServletRequest
import play.api.mvc.RawBuffer

object Helper {
  /**
   * Read config from the config param if there's the key, else get it from the global config
   * @param config [[Configuration]] Config that the value should be read from it
   * @param key [[String]] Key to search for in the config
   * @return [[String]] The value of the key in config
   */
  def readConfig(config: Configuration, key: String, default: String = null): String = {
    config.getOptional[String](key).getOrElse(default)
  }

  /**
   * Convert ArrayByte body
   * @param value [[Array]] The body to convert
   */
  final case class ArrayByte(value: Array[Byte]) {
    /**
     * Convert body to Json
     * @return [[Json]]
     */
    def toJson: Json = {
      convertToJson(this.toString)
    }

    /**
     * Convert body to string
     * @return [[String]]
     */
    override def toString: String = {
      value.map(_.toChar).mkString
    }
  }

  /**
   * Convert String to Json
   * @param string [[String]] The string to convert
   * @return [[Json]]
   */
  def convertToJson(string: String): Json = {
    io.circe.parser.parse(string).getOrElse(Json.Null)
  }

  /**
   * Read http servlet request body
   * @param request [[HttpServletRequest]] The request to read body
   * @return [[String]]
   */
  def readHttpServletRequestBody(request: HttpServletRequest): String = {
    val reader = request.getReader
    val sb = new StringBuilder
    var line = ""
    while({line = reader.readLine(); line != null}) sb.append(line)
    sb.toString
  }

  /**
   * Convert RawBuffer body
   * @param value [[RawBuffer]] The body to convert
   */
  final case class RawBufferValue(value: RawBuffer) {
    /**
     * Convert body to Json
     * @return [[Json]]
     */
    def toJson: Json = {
      io.circe.parser.parse(value.asBytes().getOrElse(ByteString("")).map(_.toChar).mkString).getOrElse(Json.Null)
    }

    /**
     * Convert body to string
     * @return [[String]]
     */
    override def toString: String = {
      value.asBytes().getOrElse(ByteString("")).map(_.toChar).mkString
    }
  }
}
