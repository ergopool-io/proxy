package controllers

import helpers.Helper
import proxy.action.MiningAction
import javax.inject._
import play.api.mvc._
import play.api.http.HttpEntity
import akka.util.ByteString
import io.circe.Json
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.core.util.Yaml
import proxy.node.{MiningCandidate, Node}
import proxy.{Config, Pool, PoolQueue, ProxyService, ProxySwagger, Response}

/**
 * Proxy pass controller
 * 
 * @constructor Create new controller for proxy passing
 * @param cc Controller component
 */
@Singleton
class ProxyController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  /**
   * Action handler for proxy passing
   *
   * @return [[Result]] Response from the node
   */
  def proxyPass: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    // Send the request to node and get its response
    val response: Response = Node.sendRequest(request.uri, request)

    val contentType = {
      if (response.contentType == "")
        "plain/text"
      else
        response.contentType
    }
    Result(
      header = ResponseHeader(response.statusCode, response.headers),
      body = HttpEntity.Strict(ByteString(response.body), Some(contentType))
    )
  }

  /**
   * Action handler to send solution to node and resend it to the pool server if it's a correct solution
   *
   * @return [[Result]] Response from the node
   */
  def sendSolution: MiningAction[RawBuffer] = MiningAction[RawBuffer] { Action(parse.raw) { implicit request: Request[RawBuffer] =>
    if (!Config.lastPoolProofWasSuccess) ProxyService.sendProofToPool()

    // Send the request to node and get its response
    val response: Response = Node.sendSolution(request)

    // Send the request to the pool server if the nodes response is 200
    if (response.statusCode == 200) {
      Pool.sendSolution(request)
    }

    Result(
      header = ResponseHeader(response.statusCode, response.headers),
      body = HttpEntity.Strict(ByteString(response.body), Some(response.contentType))
    )
  }}

  /**
   * Action handler to put "pb" in the body of response for route /mining/candidate
   *
   * @return [[Result]] Response from the node with the key "pb"
   */
  def getMiningCandidate: MiningAction[RawBuffer] = MiningAction[RawBuffer] { Action(parse.raw) { implicit request: Request[RawBuffer] =>
    if (!Config.genTransactionInProcess) {
      // Send the request to the node
      val nodeResponse: Response = Node.sendRequest(request.uri, request)

      if (nodeResponse.statusCode == 200) {
        nodeResponse.body = new MiningCandidate(nodeResponse).getResponse.map(_.toByte).toArray

      }
      Result(
        header = ResponseHeader(nodeResponse.statusCode, nodeResponse.headers),
        body = HttpEntity.Strict(ByteString(nodeResponse.body), Some(nodeResponse.contentType))
      )
    } else {
      InternalServerError
    }
  }}


  /**
   * Action handler for sending share to the pool server
   *
   * @return [[Result]] Response from the pool server
   */
  def sendShare: MiningAction[RawBuffer] = MiningAction[RawBuffer] { Action(parse.raw) { implicit request: Request[RawBuffer] =>
    if (!Config.lastPoolProofWasSuccess) ProxyService.sendProofToPool()

    try {
      // Prepare the request headers
      val reqHeaders: Seq[(String, String)] = request.headers.headers
      val body: String = ProxyService.getShareRequestBody(request)

      PoolQueue.push(s"${Config.poolConnection}${Config.poolServerSolutionRoute}", reqHeaders, body)

      // Send the request to pool server and get its response
      Ok(Json.obj().toString()).as("application/json")
    } catch {
      case _: Throwable =>
        Ok(Json.obj().toString()).as("application/json")
    }
  }}

  /**
   * Change swagger config to show pool routes
   *
   * @return [[Result]] new swagger config
   */
  def changeSwagger: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    val openApi: OpenAPI = new OpenAPIV3Parser().read(s"${Config.nodeConnection}/api-docs/swagger.conf")

    val proxyOpenAPI: OpenAPI = ProxySwagger.getProxyOpenAPI(openApi)

    val yaml = Yaml.pretty(proxyOpenAPI)

    Result(
      header = ResponseHeader(200),
      body = HttpEntity.Strict(ByteString(yaml), Some("application/json"))
    )
  }

  /**
   * Change /info to add proxy information
   *
   * @return [[Result]] new info
   */
  def changeInfo: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    // Send the request to the node
    val response: Response = Node.sendRequest(request.uri, request)

    val respBody: Json = Helper.convertBodyToJson(response.body)

    val info: Json = Helper.parseStringToJson(ProxyService.proxyInfo)

    val newResponseBody: Json = respBody.deepMerge(info)

    Result(
      header = ResponseHeader(200),
      body = HttpEntity.Strict(ByteString(newResponseBody.toString()), Some("application/json"))
    )
  }
}
