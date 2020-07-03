package controllers

import action.MiningAction
import akka.util.ByteString
import helpers.Helper
import io.circe.Json
import io.circe.syntax._
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import javax.inject._
import loggers.Logger
import node._
import play.api.http.HttpEntity
import play.api.mvc._
import proxy._

import scala.util.{Failure, Success, Try}

/**
 * Proxy pass controller
 *
 * @constructor Create new controller for proxy passing
 * @param cc Controller component
 */
@Singleton
class ProxyController @Inject()(assets: Assets, cc: ControllerComponents)(proxy: Proxy)
  extends AbstractController(cc) {
  /**
   * Action handler for proxy passing
   *
   * @return [[Result]] Response from the node
   */
  def proxyPass: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    // Send the request to node and get its response
    val response = proxy.sendRequestToNode(request)

    val contentType = {
      if (response.contentType.getOrElse("") == "")
        "plain/text"
      else
        response.contentType.get
    }

    Result(
      header = ResponseHeader(response.code, Helper.scalajHeadersToPlayHeaders(response.headers)),
      body = HttpEntity.Strict(ByteString(response.body), Some(contentType))
    )
  }

  /**
   * Action handler for reloading config
   *
   * @return
   */
  def reloadConfig: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    val success = proxy.reloadPoolQueueConfig()

    Ok(s"""{"success": $success}""").as("application/json")
  }

  /**
   * Action handler for resetting status
   *
   * @return
   */
  def resetStatus: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    val success: Boolean = {
      if (proxy.status.config.isHealthy) {
        proxy.status.reset()
        true
      } else false
    }

    if (success)
      Ok("""{"success": true}""").as("application/json")
    else
      InternalServerError(
        s"""
         |{
         |   "success": false,
         |   "message": "${proxy.status.config}"
         |}
         |""".stripMargin).as("application/json")
  }

  /**
   * Action handler to send solution to node and resend it to the pool server if it's a correct solution
   *
   * @return [[Result]] Response from the node
   */
  def sendSolution: MiningAction[RawBuffer] = MiningAction[RawBuffer](proxy) {
    Action(parse.raw) { implicit request: Request[RawBuffer] =>
      // Send the request to node and get its response
      val response = proxy.sendSolutionToNode(request)

      // Send the request to the pool server if the node's response is 200
      if (response.isSuccess) {
        proxy.sendSolution(request)
      }
      else {
        Logger.error(proxy.parseError(response))
      }

      Result(
        header = ResponseHeader(response.code, Helper.scalajHeadersToPlayHeaders(response.headers)),
        body = HttpEntity.Strict(ByteString(response.body), response.contentType)
      )
    }
  }

  /**
   * Action handler to put "pb" in the body of response for route /mining/candidate
   *
   * @return [[Result]] Response from the node with the key "pb"
   */
  def getMiningCandidate: MiningAction[RawBuffer] = MiningAction[RawBuffer](proxy) {
    Action(parse.raw) { implicit request: Request[RawBuffer] =>
      val response = proxy.getMiningCandidate
      val respHeaders = Helper.scalajHeadersToPlayHeaders(response.headers)

      Result(
        header = ResponseHeader(response.code, respHeaders),
        body = HttpEntity.Strict(ByteString(response.body), response.contentType)
      )
    }
  }


  /**
   * Action handler for sending share to the pool server
   *
   * @return [[Result]] Response from the pool server
   */
  def sendShare: MiningAction[RawBuffer] = MiningAction[RawBuffer](proxy) {
    Action(parse.raw) { implicit request: Request[RawBuffer] =>
      val shares = Share(Helper.RawBufferValue(request.body).toJson)

      proxy.sendShares(shares)

      Ok(Json.obj().toString()).as("application/json")
    }
  }

  /**
   * Change swagger config to show pool routes
   *
   * @return [[Result]] new swagger config
   */
  def changeSwagger: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    val openApi: OpenAPI = new OpenAPIV3Parser().read(s"${proxy.nodeConnection}/api-docs/swagger.conf")

    val proxyOpenAPI: OpenAPI = new ProxySwaggerBuilder(openApi)
      .addPB()
      .addShareEndpoint()
      .addSaveMnemonicEndpoint()
      .addLoadMnemonicEndpoint()
      .addConfigReload()
      .addStatusReset()
      .addTest()
      .build()

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
    val response = proxy.nodeInfo

    val respBody: Json = Helper.ArrayByte(response.body).toJson

    val info: Json = Helper.convertToJson(proxy.info)

    val newResponseBody: Json = respBody.deepMerge(info)

    Result(
      header = ResponseHeader(200),
      body = HttpEntity.Strict(ByteString(newResponseBody.toString()), Some("application/json"))
    )
  }

  // $COVERAGE-OFF$
  /**
   * Check proxy is working correctly
   *
   * @return
   */
  def test: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    Logger.messagingEnabled = true
    Logger.messages = Vector[String]()
    try {
      this.getMiningCandidate.apply(request)
    }
    catch {
      case error: Throwable =>
        Logger.error(s"Error in mining candidate - ${error.getMessage}", error)
    }
    Logger.messagingEnabled = false
    if (Logger.messages.isEmpty)
      Ok(
        """
          |{
          |   "success": true
          |}
          |""".stripMargin).as("application/json")
    else {
      InternalServerError(
        s"""
           |{
           |   "success": false,
           |   "messages": ${Logger.messages.asJson}
           |}
           |""".stripMargin).as("application/json")
    }
  }

  // $COVERAGE-ON$

  /**
   * Load mnemonic if it's not already exist
   *
   * @return [[Result]]
   */
  def loadMnemonic: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    if (proxy.mnemonic.value == null) {
      val password = Helper.RawBufferValue(request.body).toJson.hcursor.downField("pass").as[String].getOrElse("")

      proxy.mnemonic.read(password)
      proxy.status.mnemonic.setHealthy()
      Try {
        proxy.mnemonic.createAddress()
      } match {
        case Failure(exception) =>
          BadRequest(
            s"""
               |{
               |   "success": false,
               |   "message": "${exception.getMessage}"
               |}
               |""".stripMargin
          )
        case Success(_) =>
          Ok(
            """
              |{
              |   "success": true
              |}
              |""".stripMargin).as("application/json")
      }
    }
    else if (proxy.mnemonic.address == null) {
      Try {
        proxy.mnemonic.createAddress()
      } match {
        case Failure(exception) =>
          BadRequest(
            s"""
               |{
               |   "success": false,
               |   "message": "${exception.getMessage}"
               |}
               |""".stripMargin
          )
        case Success(_) =>
          Ok(
            """
              |{
              |   "success": true
              |}
              |""".stripMargin).as("application/json")
      }
    }
    else {
      Ok(
        """
          |{
          |   "success": true
          |}
          |""".stripMargin).as("application/json")
    }
  }

  /**
   * Save mnemonic to file it it's not already exists
   *
   * @return [[Result]]
   */
  def saveMnemonic: Action[RawBuffer] = Action(parse.raw) { implicit request: Request[RawBuffer] =>
    if (proxy.mnemonic.value == null) {
      BadRequest(
        """
          |{
          |   "success": false,
          |   "message": "mnemonic is not created"
          |}
          |""".stripMargin).as("application/json")
    }
    else {
      val password = Helper.RawBufferValue(request.body).toJson.hcursor.downField("pass").as[String].getOrElse("")
      if (!proxy.mnemonic.save(password)) {
        BadRequest(
          """
            |{
            |   "success": false,
            |   "message": "Mnemonic file already exists. You can remove the file if you want to change it."
            |}
            |""".stripMargin).as("application/json")
      }
      else {
        Ok(
          """
            |{
            |   "success": true
            |}
            |""".stripMargin).as("application/json")
      }
    }
  }

  def index = Action {
    Redirect("/dashboard")
  }

  def dashboard: Action[AnyContent] = assets.at("index.html")

  def assetOrDefault(resource: String): Action[AnyContent] = {
    if (resource.contains(".")) assets.at(resource) else dashboard
  }
}
