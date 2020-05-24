package controllers

import akka.util.ByteString
import helpers.Helper
import io.circe.Json
import node.{NodeClient, Share}
import org.ergoplatform.appkit.{Address, JavaHelpers, NetworkType}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.mvc.RawBuffer
import play.api.test.Helpers._
import play.api.test._
import pool.Pool
import proxy.{Mnemonic, Proxy}
import proxy.status.ProxyStatus
import scalaj.http.HttpResponse
import testservers.{TestNode, TestPoolServer}

/**
 * Check if proxy server would pass any POST or GET requests with their header and body with any route to that route of the specified node
 */
class ProxyControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {
  type Response = HttpResponse[Array[Byte]]
  var node = new TestNode(9001)
  var poolServer = new TestPoolServer(9002)

  node.startServer()
  poolServer.startServer()


  val client: NodeClient = new NodeClient

  val pool: Pool = new Pool(null)
  pool.loadConfig("0278011ec0cf5feb92d61adb51dcb75876627ace6fd9446ab4cabc5313ab7b39a7")
  var proxy: Proxy = _
  var controller: ProxyController = _

  override def beforeEach(): Unit = {
    proxy = mock[Proxy](withSettings().useConstructor(client))
    when(proxy.client).thenReturn(client)
    when(proxy.pool).thenReturn(pool)
    when(proxy.nodeConnection).thenReturn(client.connection)
    controller = new ProxyController(stubControllerComponents())(proxy)
    super.beforeEach()
  }

  override def afterAll(): Unit = {
    node.stopServer()
    poolServer.stopServer()

    super.afterAll()
  }

  /** Check ordinary routes */
  "ProxyController proxyPass Ordinary Routes" should {
    /**
     * Purpose: Check if proxy works on GET requests for ordinary routes.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake GET request to `/test/proxy` of the app and checks if response is OK.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return success for a get request" in {
      val bytes: ByteString = ByteString("")
      val f = FakeRequest(GET, "/test/proxy").withHeaders(("Content-Type", "")).withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      when(proxy.sendRequestToNode(f)).thenCallRealMethod()
      val response = controller.proxyPass.apply(f)

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must include ("{\"success\": true}")
    }

    /**
     * Purpose: Check if proxy works on POST requests for ordinary routes.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake POST request to `/test/proxy` of the app and checks if response is OK.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return success for a post request" in {
      val bytes: ByteString = ByteString("")
      val f = FakeRequest(POST, "/test/proxy").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      when(proxy.sendRequestToNode(f)).thenCallRealMethod()
      val response = controller.proxyPass.apply(f)

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must include ("{\"success\": true}")
    }
  }

  /** Check mnemonic */
  "ProxyController Mnemonic" should {
    /**
     * Purpose: return success when all ok
     * Scenario: Sends a request to load method.
     * Test Conditions:
     * * status is OK
     * * success in response is true
     */
    "return success when mnemonic is loaded and ok" in {
      val mnemonic = mock[Mnemonic]
      when(mnemonic.value).thenReturn("")
      val address = JavaHelpers.createP2PKAddress(Address.create(client.minerAddress).getPublicKey, NetworkType.TESTNET.networkPrefix)
      when(mnemonic.address).thenReturn(address)
      when(proxy.mnemonic).thenReturn(mnemonic)
      val bytes: ByteString = ByteString("")
      val fakeRequest = FakeRequest(POST, "/proxy/mnemonic/load").withHeaders("Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.loadMnemonic(fakeRequest)

      contentAsString(response).replaceAll("\\s", "") mustBe
        """
          |{
          |   "success": true
          |}
          |""".stripMargin.replaceAll("\\s", "")
      status(response) mustBe OK
    }

    /**
     * Purpose: Check that response success when address can be created.
     * Scenario: Sends a request to load method.
     * Test Conditions:
     * * status is OK
     * * success in response is true
     */
    "return success when mnemonic is loaded and address is created" in {
      val mnemonic = mock[Mnemonic]
      when(proxy.mnemonic).thenReturn(mnemonic)
      val bytes: ByteString = ByteString("""{"pass": "right password"}""")
      val fakeRequest = FakeRequest(POST, "/proxy/mnemonic/load").withHeaders("Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val proxyStatus: ProxyStatus = new ProxyStatus
      proxyStatus.mnemonic.setUnhealthy()
      when(proxy.status).thenReturn(proxyStatus)
      val response = controller.loadMnemonic(fakeRequest)

      verify(mnemonic, times(1)).read("right password")
      proxy.status.isHealthy mustBe true
      contentAsString(response).replaceAll("\\s", "") mustBe
        """
          |{
          |   "success": true
          |}
          |""".stripMargin.replaceAll("\\s", "")
      status(response) mustBe OK
    }

    /**
     * Purpose: value is null so mnemonic can be loaded with the right password.
     * Scenario: Sends a request to load method.
     * Test Conditions:
     * * status is OK
     * * success in response is true
     */
    "return success when mnemonic is not loaded and password is ok" in {
      val mnemonic = mock[Mnemonic]
      when(mnemonic.value).thenReturn(null)
      when(proxy.mnemonic).thenReturn(mnemonic)
      val proxyStatus: ProxyStatus = new ProxyStatus
      proxyStatus.mnemonic.setUnhealthy()
      when(proxy.status).thenReturn(proxyStatus)
      val bytes: ByteString = ByteString("""{"pass": "right password"}""")
      val fakeRequest = FakeRequest(POST, "/proxy/mnemonic/load").withHeaders("Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.loadMnemonic(fakeRequest)

      proxy.status.isHealthy mustBe true
      verify(mnemonic, times(1)).read("right password")
      contentAsString(response).replaceAll("\\s", "") mustBe
        """
          |{
          |   "success": true
          |}
          |""".stripMargin.replaceAll("\\s", "")
      status(response) mustBe OK
    }

    /**
     * Purpose: mnemonic can not be loaded with wrong password.
     * Scenario: Sends a request to load method.
     * Test Conditions:
     * * throws wrong password exception
     */
    "throw exception, password is wrong" in {
      val bytes: ByteString = ByteString("""{"pass": "wrong password"}""")
      val mnemonic = mock[Mnemonic]
      when(mnemonic.value).thenReturn(null)
      when(mnemonic.read("wrong password")).thenThrow(classOf[mnemonic.WrongPassword])
      when(proxy.mnemonic).thenReturn(mnemonic)
      val fakeRequest = FakeRequest(POST, "/proxy/mnemonic/load").withHeaders("Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      try {
        controller.loadMnemonic(fakeRequest)
        fail()

      } catch {
        case _: mnemonic.WrongPassword =>
      }

    }

    /**
     * Purpose: mnemonic file does not exist so it can not be loaded.
     * Scenario: Sends a request to load method.
     * Test Conditions:
     * * throws exception
     */
    "throw exception, mnemonic file does not exist" in {
      val bytes: ByteString = ByteString("""{"pass": "wrong password"}""")
      val mnemonic = mock[Mnemonic]
      when(mnemonic.value).thenReturn(null)
      when(mnemonic.read("wrong password")).thenThrow(classOf[mnemonic.FileDoesNotExists])
      when(proxy.mnemonic).thenReturn(mnemonic)
      val fakeRequest = FakeRequest(POST, "/proxy/mnemonic/load").withHeaders("Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      try {
        controller.loadMnemonic(fakeRequest)
        fail()

      } catch {
        case _: mnemonic.FileDoesNotExists =>
      }
    }

    /**
     * Purpose: can not save an unloaded mnemonic
     * Scenario: Sends a request to save method.
     * Test Conditions:
     * * throws exception
     */
    "throw exception in save, value is null" in {
      val bytes: ByteString = ByteString("""{"pass": "some password"}""")
      val mnemonic = mock[Mnemonic]
      when(mnemonic.value).thenReturn(null)
      when(proxy.mnemonic).thenReturn(mnemonic)
      val fakeRequest = FakeRequest(POST, "/proxy/mnemonic/save").withHeaders("Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.saveMnemonic(fakeRequest)
      status(response) mustBe BAD_REQUEST
    }

    /**
     * Purpose: mnemonic file already exists, can not save.
     * Scenario: Sends a request to save method.
     * Test Conditions:
     * * throws exception
     */
    "throw exception in save, mnemonic file already exists" in {
      val bytes: ByteString = ByteString("""{"pass": "some password"}""")
      val mnemonic = mock[Mnemonic]
      when(mnemonic.value).thenReturn("")
      when(mnemonic.save("some password")).thenReturn(false)
      when(proxy.mnemonic).thenReturn(mnemonic)
      val fakeRequest = FakeRequest(POST, "/proxy/mnemonic/save").withHeaders("Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.saveMnemonic(fakeRequest)
      status(response) mustBe BAD_REQUEST
    }

    /**
     * Purpose: all ok, save mnemonic is successful
     * Scenario: Sends a request to save method.
     * Test Conditions:
     * * status is OK
     */
    "all ok, must return success" in {
      val bytes: ByteString = ByteString("""{"pass": "some password"}""")
      val mnemonic = mock[Mnemonic]
      when(mnemonic.value).thenReturn("")
      when(mnemonic.save("some password")).thenReturn(true)
      when(proxy.mnemonic).thenReturn(mnemonic)
      val fakeRequest = FakeRequest(POST, "/proxy/mnemonic/save").withHeaders("Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.saveMnemonic(fakeRequest)
      status(response) mustBe OK
    }

  }

  /** Check solution requests */
  "ProxyController reloadConfig" should {
    /**
     * Purpose: Check that config will be reloaded.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: Sends a reload request to controller
     * Test Conditions:
     * * status is OK
     * * Content-Type is application/json
     */
    "return 200 on reloading config" in {
      val bytes: ByteString = ByteString("")
      when(proxy.reloadPoolQueueConfig()).thenReturn(true)
      val fakeRequest = FakeRequest(POST, "/config/reload").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.reloadConfig.apply(fakeRequest)

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
    }
  }

  /** Check get candidate requests */
  "ProxyController getMiningCandidate" should {
    /**
     * Purpose: get mining candidate
     * Prerequisites: None
     * Scenario: It sends a fake GET request to `/mining/candidate` and passes it to the app.
     * Test Conditions:
     * * status is OK
     */
    "return mining candidate with valid content type" in {
      val bytes: ByteString = ByteString("")
      val proxyStatus: ProxyStatus = new ProxyStatus
      proxyStatus.reset()
      when(proxy.status).thenReturn(proxyStatus)
      val res: Response = HttpResponse[Array[Byte]](bytes.toArray, 200, Map("Content-Type" -> Vector("application/json")))
      when(proxy.getMiningCandidate).thenReturn(res)
      val fakeRequest = FakeRequest(GET, "/mining/candidate").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.getMiningCandidate.apply(fakeRequest)

      verify(proxy, times(1)).getMiningCandidate
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
    }
  }

  /** Check solution requests */
  "ProxyController sendSolution" should {
    /**
     * Purpose: Check solution won't be sent to pool server if status is 400.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake POST request with an invalid body to `/mining/solution` to the app.
     *           As the solution is invalid, status would be 400 so it won't send the request to the pool server.
     * Test Conditions:
     * * gotSolution is false
     * * status is `400`
     * * Content-Type is `application/json`
     */
    "return 400 status code on an invalid solution" in {
      val bytes: ByteString = ByteString("")
      val proxyStatus: ProxyStatus = new ProxyStatus
      proxyStatus.reset()
      when(proxy.status).thenReturn(proxyStatus)
      val res: Response = HttpResponse[Array[Byte]](Array(), 400, Map("Content-Type" -> Vector("application/json")))
      when(proxy.sendSolutionToNode(any())).thenReturn(res)
      val fakeRequest = FakeRequest(POST, "/mining/solution").withHeaders("api_key" -> "some string", "Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.sendSolution.apply(fakeRequest)

      verify(proxy, times(0)).sendSolution(any())

      status(response) mustBe BAD_REQUEST
      contentType(response) mustBe Some("application/json")
    }


    /**
     * Purpose: Check solution will be sent to pool server if status is 200.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake POST request to `/mining/solution` to the app.
     *           Status is 200 so it should send the request to the pool server.
     * Test Conditions:
     * * TestPoolServer.servlets.gotSolution is true
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return 200 status code on correct solution" in {
      val bytes: ByteString = ByteString("""{"success": true}""")
      val proxyStatus: ProxyStatus = new ProxyStatus
      proxyStatus.reset()
      when(proxy.status).thenReturn(proxyStatus)
      val res: Response = HttpResponse[Array[Byte]](bytes.toArray, 200, Map("Content-Type" -> Vector("application/json")))
      when(proxy.sendSolutionToNode(any())).thenReturn(res)
      val fakeRequest = FakeRequest(POST, "/mining/solution").withHeaders("api_key" -> "some string", "Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.sendSolution.apply(fakeRequest)

      verify(proxy, times(1)).sendSolution(any())
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must include ("{\"success\": true}")
    }
  }

  /** Check share requests */
  "ProxyController sendShare" should {

    /**
     * Purpose: Check if share will be sent to the pool server.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake POST request to `/mining/share` to the app.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success":true}`
     */
    "return 200 status code from pool server on new share" in {
      val body: String =
        """
          |{
          |  "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
          |  "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
          |  "n": "0000000010C006CF",
          |  "d": 4196585670338033714759641235444284559441802073009721710293850518130743229130
          |}
          |""".stripMargin
      val shares = Share(Helper.convertToJson(body))
      val bytes: ByteString = ByteString(body)
      val fakeRequest = FakeRequest(POST, "/mining/share").withHeaders("api_key" -> "some string", "Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val proxyStatus: ProxyStatus = new ProxyStatus
      proxyStatus.reset()
      when(proxy.status).thenReturn(proxyStatus)
      val response = controller.sendShare.apply(fakeRequest)

      verify(proxy, times(1)).sendShares(shares)
      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response).replaceAll("\\s", "") must include ("{}")
    }
  }

  /** Check Info */
  "ProxyController changeInfo" should {
    val greenInfo: String =
      """
        |{
        |    "pool" : {
        |      "connection" : "http://localhost:9002",
        |      "config" : {
        |        "wallet" : "3WvrVTCPJ1keSdtqNL5ayzQ62MmTNz4Rxq7vsjcXgLJBwZkvHrGa",
        |        "difficulty_factor" : 10.0,
        |        "transaction_request_value" : 67500000000,
        |        "max_chunk_size": 10
        |      }
        |    },
        |    "status" : {
        |      "health": "GREEN"
        |    }
        |}
        |""".stripMargin.replaceAll("\\s", "")

    val redInfo: String =
      """
        |{
        |    "pool" : {
        |      "connection" : "http://localhost:9002",
        |      "config" : {
        |        "wallet" : "3WvrVTCPJ1keSdtqNL5ayzQ62MmTNz4Rxq7vsjcXgLJBwZkvHrGa",
        |        "difficulty_factor" : 10.0,
        |        "transaction_request_value" : 67500000000,
        |        "max_chunk_size": 10
        |      }
        |    },
        |    "status" : {
        |      "health": "RED"
        |      "reason": {
        |        "walletLock": "RED - Wallet is lock"
        |     }
        |   }
        |}
        |""".stripMargin
    /**
     * Purpose: Check the proxy info is in /info.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake GET request to `/info` to the app.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content has the proxy info
     */
    "return info with green proxy" in {
      val proxyStatus: ProxyStatus = new ProxyStatus
      proxyStatus.reset()
      when(proxy.status).thenReturn(proxyStatus)
      when(proxy.info).thenCallRealMethod()
      when(proxy.nodeInfo).thenCallRealMethod()
      val bytes: ByteString = ByteString("")
      val response = controller.changeInfo.apply(FakeRequest(GET, "/info").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      val proxyField = Helper.convertToJson(contentAsString(response)).hcursor.downField("proxy").as[Json].getOrElse(Json.Null)
      proxyField mustBe Helper.convertToJson(greenInfo)
    }

    /**
     * Purpose: Check the proxy info is in /info when status is red.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake GET request to `/info` to the app.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content has the proxy info
     */
    "return info with red proxy" in {
      val proxyStatus: ProxyStatus = new ProxyStatus
      proxyStatus.walletLock.setUnhealthy()
      when(proxy.status).thenReturn(proxyStatus)
      when(proxy.info).thenCallRealMethod()
      when(proxy.nodeInfo).thenCallRealMethod()
      val bytes: ByteString = ByteString("")
      val response = controller.changeInfo.apply(FakeRequest(GET, "/info").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      val proxyField = Helper.convertToJson(contentAsString(response)).hcursor.downField("proxy").as[Json].getOrElse(Json.Null)
      proxyField mustBe Helper.convertToJson(redInfo)
    }
  }

  /** Check status */
  "ProxyController resetStatus" should {
    val success: String =
      """
        |{"success": true}
        |""".stripMargin.replaceAll("\\s", "")

    val failed: String =
      """
        |{
        |   "success": false,
        |   "message": "RED - Error getting config from the pool"
        |}
        |""".stripMargin.replaceAll("\\s", "")
    /**
     * Purpose: Check the proxy status will change after
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake POST request to `/status/reset` to the app.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is {"success": true}
     */
    "check proxy status is reset" in {
      val proxyStatus: ProxyStatus = new ProxyStatus
      proxyStatus.walletLock.setUnhealthy()
      when(proxy.status).thenReturn(proxyStatus)
      val bytes: ByteString = ByteString("")
      val response = controller.resetStatus.apply(FakeRequest(POST, "/status/reset").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response).replaceAll("\\s", "") mustBe success
    }

    /**
     * Purpose: Check the proxy status won't change if category is Config
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake POST request to `/status/reset` to the app.
     * Test Conditions:
     * * status is `500`
     * * Content-Type is `application/json`
     * * Content is {"success": false,"message": "This is test"}
     */
    "check proxy status is not reset if status category is Config" in {
      val proxyStatus: ProxyStatus = new ProxyStatus
      proxyStatus.config.setUnhealthy()
      when(proxy.status).thenReturn(proxyStatus)
      val bytes: ByteString = ByteString("")
      val response = controller.resetStatus.apply(FakeRequest(POST, "/status/reset").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      status(response) mustBe INTERNAL_SERVER_ERROR
      contentType(response) mustBe Some("application/json")
      contentAsString(response).replaceAll("\\s", "") mustBe failed
    }
  }

  /** Check swagger */
  "ProxyController changeSwagger" should {

    /**
     * Purpose: Check new swagger config.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake GET request to `/api-docs/swagger.conf` to the app.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content had /mining/share and pb in /mining/candidate
     */
    "return change swagger config" in {
      val bytes: ByteString = ByteString("")
      val response = controller.changeSwagger.apply(FakeRequest(GET, "/api-docs/swagger.conf").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must include
      """
        |openapi: 3.0.2
        |info:
        |  title: Ergo Node API
        |  description: API docs for Ergo Node. Models are shared between all Ergo products
        |  contact:
        |    name: Ergo Platform Team
        |    url: https://ergoplatform.org
        |    email: ergoplatform@protonmail.com
        |  license:
        |    name: CC0 1.0 Universal
        |    url: https://raw.githubusercontent.com/ergoplatform/ergo/master/LICENSE
        |  version: 3.1.2
        |servers:
        |- url: /
        |paths:
        |  /info:
        |    get:
        |      tags:
        |      - info
        |      summary: Get the information about the Node
        |      operationId: getNodeInfo
        |      responses:
        |        200:
        |          description: Node info object
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/NodeInfo'
        |        default:
        |          description: Error
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ApiError'
        |  /proxy/test:
        |    get:
        |      tags:
        |      - proxy
        |      summary: Test proxy is working
        |      responses:
        |        200:
        |          description: Proxy is working
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ProxySuccess'
        |        500:
        |          description: Exception happened when testing proxy
        |          content:
        |            application/json:
        |              schema:
        |                required:
        |                - messages
        |                - success
        |                type: object
        |                properties:
        |                  success:
        |                    type: boolean
        |                    description: True if operation was successful
        |                    example: false
        |                  message:
        |                    type: array
        |                    description: List of reasons of failure
        |                    items:
        |                      type: string
        |                      description: error messages during the test
        |        default:
        |          description: Exception happened when testing proxy
        |          content:
        |            application/json:
        |              schema:
        |                required:
        |                - messages
        |                - success
        |                type: object
        |                properties:
        |                  success:
        |                    type: boolean
        |                    description: True if operation was successful
        |                    example: false
        |                  message:
        |                    type: array
        |                    description: List of reasons of failure
        |                    items:
        |                      type: string
        |                      description: error messages during the test
        |  /proxy/status/reset:
        |    post:
        |      tags:
        |      - proxy
        |      summary: Reset status of proxy
        |      responses:
        |        200:
        |          description: Status reset successfully
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ProxySuccess'
        |        500:
        |          description: Reset status failed
        |          content:
        |            application/json:
        |              schema:
        |                required:
        |                - message
        |                - success
        |                type: object
        |                properties:
        |                  success:
        |                    type: boolean
        |                    description: True if operation was successful
        |                    example: false
        |                  message:
        |                    type: string
        |                    description: reason of failure in operation
        |                    example: Something happened
        |        default:
        |          description: Reset status failed
        |          content:
        |            application/json:
        |              schema:
        |                required:
        |                - message
        |                - success
        |                type: object
        |                properties:
        |                  success:
        |                    type: boolean
        |                    description: True if operation was successful
        |                    example: false
        |                  message:
        |                    type: string
        |                    description: reason of failure in operation
        |                    example: Something happened
        |  /proxy/config/reload:
        |    post:
        |      tags:
        |      - proxy
        |      summary: Reload proxy config from the pool server
        |      responses:
        |        200:
        |          description: Config reloaded
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ProxySuccess'
        |        default:
        |          description: Config reloaded
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ProxySuccess'
        |  /proxy/mnemonic/load:
        |    post:
        |      tags:
        |      - proxy
        |      summary: Load mnemonic
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              properties:
        |                pass:
        |                  type: string
        |                  description: Password of the mnemonic file
        |                  example: My password
        |      responses:
        |        200:
        |          description: Mnemonic has been loaded successfully
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ProxySuccess'
        |        400:
        |          description: Couldn't load mnemonic
        |          content:
        |            application/json:
        |              schema:
        |                required:
        |                - message
        |                - success
        |                type: object
        |                properties:
        |                  success:
        |                    type: boolean
        |                    description: True if operation was successful
        |                    example: false
        |                  message:
        |                    type: string
        |                    description: reason of failure in operation
        |                    example: Password is wrong. Send the right one or remove mnemonic
        |                      file.
        |        default:
        |          description: Couldn't load mnemonic
        |          content:
        |            application/json:
        |              schema:
        |                required:
        |                - message
        |                - success
        |                type: object
        |                properties:
        |                  success:
        |                    type: boolean
        |                    description: True if operation was successful
        |                    example: false
        |                  message:
        |                    type: string
        |                    description: reason of failure in operation
        |                    example: Password is wrong. Send the right one or remove mnemonic
        |                      file.
        |  /proxy/mnemonic/save:
        |    post:
        |      tags:
        |      - proxy
        |      summary: Save mnemonic to file using the password
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              properties:
        |                pass:
        |                  type: string
        |                  description: Password to save mnemonic to file using it
        |                  example: My password
        |      responses:
        |        200:
        |          description: Mnemonic has been saved into the file successfully
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ProxySuccess'
        |        400:
        |          description: Couldn't save mnemonic
        |          content:
        |            application/json:
        |              schema:
        |                required:
        |                - message
        |                - success
        |                type: object
        |                properties:
        |                  success:
        |                    type: boolean
        |                    description: True if operation was successful
        |                    example: false
        |                  message:
        |                    type: string
        |                    description: reason of failure in operation
        |                    example: Mnemonic file already exists. You can remove the file
        |                      if you want to change it.
        |        default:
        |          description: Couldn't save mnemonic
        |          content:
        |            application/json:
        |              schema:
        |                required:
        |                - message
        |                - success
        |                type: object
        |                properties:
        |                  success:
        |                    type: boolean
        |                    description: True if operation was successful
        |                    example: false
        |                  message:
        |                    type: string
        |                    description: reason of failure in operation
        |                    example: Mnemonic file already exists. You can remove the file
        |                      if you want to change it.
        |  /mining/candidate:
        |    get:
        |      tags:
        |      - mining
        |      summary: Request block candidate
        |      operationId: miningRequestBlockCandidate
        |      responses:
        |        "200":
        |          description: External candidate
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ExternalCandidateBlock'
        |        default:
        |          description: Error
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ApiError'
        |      security:
        |      - ApiKeyAuth:
        |        - api_key
        |  /mining/rewardAddress:
        |    get:
        |      tags:
        |      - mining
        |      summary: Read miner reward address
        |      operationId: miningReadMinerRewardAddress
        |      responses:
        |        "200":
        |          description: External candidate
        |          content:
        |            application/json:
        |              schema:
        |                required:
        |                - rewardAddress
        |                type: object
        |        default:
        |          description: Error
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ApiError'
        |      security:
        |      - ApiKeyAuth:
        |        - api_key
        |  /mining/share:
        |    post:
        |      tags:
        |      - mining
        |      summary: Submit share for current candidate
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              $ref: '#/components/schemas/PowSolutions'
        |        required: true
        |      responses:
        |        "200":
        |          description: Share is valid
        |        "500":
        |          description: Error
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ApiError'
        |        default:
        |          description: Error
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ApiError'
        |      security:
        |      - ApiKeyAuth:
        |        - '[api_key]'
        |  /mining/solution:
        |    post:
        |      tags:
        |      - mining
        |      summary: Submit solution for current candidate
        |      operationId: miningSubmitSolution
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              $ref: '#/components/schemas/PowSolutions'
        |        required: true
        |      responses:
        |        "200":
        |          description: Solution is valid
        |        "400":
        |          description: Solution is invalid
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ApiError'
        |        default:
        |          description: Error
        |          content:
        |            application/json:
        |              schema:
        |                $ref: '#/components/schemas/ApiError'
        |      security:
        |      - ApiKeyAuth:
        |        - api_key
        |components:
        |  schemas:
        |    ExternalCandidateBlock:
        |      required:
        |      - b
        |      - msg
        |      - pb
        |      - pk
        |      type: object
        |      properties:
        |        msg:
        |          type: string
        |          description: Base16-encoded block bytes without pow
        |          example: 0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5
        |        b:
        |          type: integer
        |          example: 9876543210
        |        pk:
        |          type: string
        |          description: Base16-encoded public key
        |          example: 0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5
        |        pb:
        |          type: Integer
        |          example: 9876543210
        |      description: Candidate block info for external miner
        |    ApiError:
        |      required:
        |      - detail
        |      - error
        |      - reason
        |      type: object
        |      properties:
        |        error:
        |          type: integer
        |          description: Error code
        |          example: 500
        |        reason:
        |          type: string
        |          description: String error code
        |          example: Internal server error
        |        detail:
        |          type: string
        |          description: Detailed error description
        |          nullable: true
        |    ProxySuccess:
        |      required:
        |      - success
        |      type: object
        |      properties:
        |        success:
        |          type: boolean
        |          description: True if operation was successful
        |          example: true
        |  securitySchemes:
        |    ApiKeyAuth:
        |      type: apiKey
        |      name: api_key
        |      in: header
        |""".stripMargin
    }
  }
}
