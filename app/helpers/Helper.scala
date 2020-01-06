package helpers

import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import com.typesafe.config.Config
import io.circe.Json
import javax.servlet.http.HttpServletRequest
import play.api.mvc.RawBuffer
import scalaj.http.HttpResponse

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


  /**
   * Convert response body to [[Json]]
   * @param body response body
   * @return [[Json]]
   */
  def convertBodyToJson(body: Array[Byte]): Json = {
    io.circe.parser.parse(body.map(_.toChar).mkString).getOrElse(Json.Null)
  }

  /**
   * Convert String to Json
   * @param string [[String]] The string to convert
   * @return [[Json]]
   */
  def parseStringToJson(string: String): Json = {
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
   * @param body [[RawBuffer]] The body to convert
   */
  final case class ConvertRaw(body: RawBuffer) {
    /**
     * Convert body to Json
     * @return [[Json]]
     */
    def toJson: Json = {
      io.circe.parser.parse(body.asBytes().getOrElse(ByteString("")).map(_.toChar).mkString).getOrElse(Json.Null)
    }

    /**
     * Convert body to string
     * @return [[String]]
     */
    override def toString: String = {
      body.asBytes().getOrElse(ByteString("")).map(_.toChar).mkString
    }
  }

  /**
   * Get string form of HttpResponse body
   * @param response [[HttpResponse]] the response to read body
   * @return [[String]]
   */
  def getHttpResponseBody(response: HttpResponse[Array[Byte]]): String = {
    response.body.map(_.toChar).mkString
  }
}
