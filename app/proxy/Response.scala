package proxy

import scalaj.http.HttpResponse

/**
 * A structure for responses from the node
 *
 * @constructor Create a new object containing the params
 * @param statusCode [[Int]] The status code of the response
 * @param headers [[Map[String, String]]] The headers of the response
 * @param body [[Array[Byte]]] The body of the response
 * @param contentType [[String]] The content type of the response
 */
case class Response(statusCode: Int, headers: Map[String, String], var body: Array[Byte], contentType: String)

/**
 * A structure for responses from the node
 *
 * @constructor Create a new object containing the params
 */
object Response {
  /**
   *
   * @param statusCode [[Int]] The status code of the response
   * @param headers [[Map[String, String]]] The headers of the response
   * @param body [[Array[Byte]]] The body of the response
   * @param contentType [[String]] The content type of the response
   * @return
   */
  def apply(statusCode: Int, headers: Map[String, String], body: Array[Byte], contentType: String): Response = {
    new Response(statusCode, headers, body, contentType)
  }

  /**
   * Create Response with a HttpResponse
   * @param response [[HttpResponse]] response
   * @return
   */
  def apply(response: HttpResponse[Array[Byte]]): Response = {
    val statusCode = response.code

    val respHeaders: Map[String, String] = response.headers.map({
      case (key, value) =>
        key -> value.mkString(" ")
    })

    // Remove the ignored headers
    val contentType = respHeaders.getOrElse("Content-Type", "")
    val body = response.body

    new Response(statusCode, respHeaders, body, contentType)
  }
}
