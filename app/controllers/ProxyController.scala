package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.Configuration
import play.api.http.HttpEntity
import com.typesafe.config.ConfigFactory
import scalaj.http.{Http, HttpResponse}
import akka.util.ByteString
import play.api.libs.json.{JsValue}

/**
 * Proxy pass controller
 * 
 * @constructor create new controller for proxy passing
 * @param cc controller component
 * @param config configuration object
 */
@Singleton
class ProxyController @Inject()(cc: ControllerComponents)(config: Configuration) extends AbstractController(cc) {

  /**
   * Returns response of the clients requests from node
   * 
   * @param Request[AnyContent] request
   * @return Result response from node
   */
  def proxy() = Action { implicit request: Request[AnyContent] =>
    // Prepare request header
    val reqHeaders : Seq[(String, String)] = request.headers.headers
    
    // Set server attributes
    var optHost : Option[String] = config.getString("ergo.node.host")
    var optPort : Option[String] = config.getString("ergo.node.api.port")

    // Set node params
    val host : String = if (optHost.isDefined) optHost.get else ConfigFactory.load().getString("ergo.node.host")
    val port : String = if (optPort.isDefined) optPort.get else ConfigFactory.load().getString("ergo.node.api.port")

    // Send incoming request to node
    val response : HttpResponse[Array[Byte]] = {
      if (request.method == "GET") {
        Http(host + ":" + port + request.uri).headers(reqHeaders).asBytes
      }
      else {
        // Prepare request body
        val rawBody : Option[JsValue] = request.body.asJson
        val body : String = if (rawBody.isDefined) rawBody.get.toString else ""
  
        Http(host + ":" + port + request.uri).headers(reqHeaders).postData(body).asBytes
      }
    }
    
    var respHeaders : Map[String, String] = response.headers.map({
      case (key, value) => 
        key -> value.mkString(" ")
    })

    // Remove ignored headers
    val contentType : String = respHeaders.getOrElse("Content-Type", "")
    val filteredHeaders : Map[String, String] = respHeaders.removed("Content-Type").removed("Content-Length")

    Result(
      header = ResponseHeader(response.code, filteredHeaders),
      body = HttpEntity.Strict(ByteString(response.body), Some(contentType))
    )
  }
}
