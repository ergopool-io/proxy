package controllers

import loggers.ServerLogger
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.Configuration
import play.api.http.HttpEntity
import com.typesafe.config.ConfigFactory
import scalaj.http.{Http, HttpResponse}
import akka.util.ByteString
import io.circe.syntax._
import io.circe.parser.parse
import io.circe.Json

/**
 * Proxy pass controller
 * 
 * @constructor Create new controller for proxy passing
 * @param cc Controller component
 * @param config Configuration object
 */
@Singleton
class ProxyController @Inject()(cc: ControllerComponents)(config: Configuration)(logger: ServerLogger) extends AbstractController(cc) {

  // Set pool server params
  val poolConnection: String = config.getOptional[String]("pool.server.connection").getOrElse(ConfigFactory.load().getString("pool.server.connection"))
  
  // Set the node params
  val nodeConnection: String = config.getOptional[String]("ergo.node.connection").getOrElse(ConfigFactory.load().getString("ergo.node.connection"))
  
  /**
   * A structure for responses from the node
   * 
   * @constructor Create a new object containing the params
   * @param statusCode [[Int]] The status code of the response
   * @param headers [[Map[String, String]]] The headers of the response
   * @param body [[Array[Byte]]] The body of the response
   * @param contentType [[String]] The content type of the response
   */
  case class ProxyResponse(statusCode: Int, headers: Map[String, String],var body: Array[Byte], contentType: String)

  /**
   * Send a request to a url with its all headers and body
   * 
   * @param url [[String]] Servers url
   * @param request [[Request[AnyContent]]] The request to send
   * @return [[HttpResponse[Array[Byte]]]] Response from the server
   */ 
  private def sendRequest(url: String, request: Request[AnyContent]): HttpResponse[Array[Byte]] = {
    
    // Prepare the request headers
    val reqHeaders: Seq[(String, String)] = request.headers.headers

    // Send the incoming request to node
    val response: HttpResponse[Array[Byte]] = {
      if (request.method == "GET") {
        Http(url).headers(reqHeaders).asBytes
      }
      else {
        // Prepare the request body
        val body: String = request.body.toString
  
        Http(url).headers(reqHeaders).postData(body).asBytes
      }
    }
    response
  }

  /**
   * Prepare and return the response with its all headers and body
   * 
   * @param response [[HttpResponse[Array[Byte]]]] The request to send
   * @return [[ProxyResponse]] Prepared response
   */ 
  private def sendResponse(response: HttpResponse[Array[Byte]]): ProxyResponse = {
    
    // Convert the headers to Map[String, String] type
    var respHeaders: Map[String, String] = response.headers.map({
      case (key, value) => 
        key -> value.mkString(" ")
    })

    // Remove the ignored headers
    val contentType: String = respHeaders.getOrElse("Content-Type", "")
    val filteredHeaders: Map[String, String] = respHeaders.removed("Content-Type").removed("Content-Length")

    // Return the response
    ProxyResponse(
      statusCode = response.code, 
      headers = filteredHeaders,
      body = response.body,
      contentType = contentType
    )
  }
  
  /**
   * Returns the response of the clients requests from the node
   * 
   * @param request [[Request[AnyContent]]] The request to send
   * @return [[ProxyResponse]] Response from the node
   */
  private def proxy(request: Request[AnyContent], config: Configuration = this.config): ProxyResponse = {
    
    // Log the request
    logger.logRequest(request)
    
    // Send the request to the node
    val response: HttpResponse[Array[Byte]] = this.sendRequest(s"${this.nodeConnection}${request.uri}", request)
    
    // Log the response
    logger.logResponse(response)
    
    // Return the response
    this.sendResponse(response)
  }

  /**
   * Action handler for proxy passing
   * 
   * @return [[Result]] Response from the node
   */
  def proxyPass() = Action { implicit request: Request[AnyContent] =>
    
    // Send the request to node and get its response
    val response: ProxyResponse = this.proxy(request)

    Result(
      header = ResponseHeader(response.statusCode, response.headers),
      body = HttpEntity.Strict(ByteString(response.body), Some(response.contentType))
    )
  }

  /**
   * Action handler to send solution to node and resend it to the pool server if it's a correct solution
   * 
   * @return [[Result]] Response from the node
   */
  def solution() = Action { implicit request: Request[AnyContent] =>

    // Send the request to node and get its response
    val response: ProxyResponse = this.proxy(request)

    // Send the request to the pool server if the nodes response is 200
    if (response.statusCode == 200) {
      this.sendRequest(s"${this.poolConnection}", request)
    }

    Result(
      header = ResponseHeader(response.statusCode, response.headers),
      body = HttpEntity.Strict(ByteString(response.body), Some(response.contentType))
    )
  }

  /**
   * Action handler to put "pb" in the body of response for route /mining/candidate
   * 
   * @return [[Result]] Response from the node with the key "pb"
   */
  def getMiningCandidate() = Action { implicit request: Request[AnyContent] =>

    // Log the request
    logger.logRequest(request)
    
    // Send the request to the node
    var response: HttpResponse[Array[Byte]] = this.sendRequest(s"${this.nodeConnection}${request.uri}", request)
    
    // Get the response in ProxyResponse format
    var preparedResponse: ProxyResponse = this.sendResponse(response)

    val newResponse: HttpResponse[Array[Byte]] = {
      if (preparedResponse.statusCode == 200) {
        // Get the pool difficulty from the config and put it in the body
        val poolDifficulty: BigInt = BigInt(config.getOptional[String]("pool.server.difficulty").getOrElse(ConfigFactory.load().getString("pool.server.difficulty")))
        val body: Json = io.circe.parser.parse((response.body.map(_.toChar)).mkString).getOrElse(Json.Null).deepMerge(Json.obj("pb" -> poolDifficulty.asJson))

        preparedResponse.body = body.toString.getBytes
        new HttpResponse[Array[Byte]](body.toString.getBytes, response.code, response.headers)
      }
      else {
        response
      }
    }
    
    // Log the response
    logger.logResponse(newResponse)
    
    Result(
      header = ResponseHeader(preparedResponse.statusCode, preparedResponse.headers),
      body = HttpEntity.Strict(ByteString(preparedResponse.body), Some(preparedResponse.contentType))
    )
  }
}
