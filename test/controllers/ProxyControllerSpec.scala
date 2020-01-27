package controllers

import akka.util.ByteString
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.BeforeAndAfterAll
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.mvc.RawBuffer
import proxy.node.Node
import proxy.{Config, PoolShareQueue}
import proxy.status.{ProxyStatus, StatusType}
import testservers.{NodeServlets, PoolServerServlets, TestNode, TestPoolServer}

import scala.util.{Failure, Try}

/**
 * Check if proxy server would pass any POST or GET requests with their header and body with any route to that route of the specified node
 */
class ProxyControllerSpec extends PlaySpec with BeforeAndAfterAll {
  val testNodeConnection: String = Config.nodeConnection
  val testPoolServerConnection: String = Config.poolConnection

  val node: TestNode = new TestNode(testNodeConnection.split(":").last.toInt)
  val pool: TestPoolServer = new TestPoolServer(testPoolServerConnection.split(":").last.toInt)

  node.startServer()
  pool.startServer()

  Config.loadPoolConfig()

  val controller: ProxyController = new ProxyController(stubControllerComponents())

  override def beforeAll(): Unit = {
    while (!ProxyStatus.isHealthy) Thread.sleep(1000)
  }


  override def afterAll(): Unit = {
    node.stopServer()

    PoolShareQueue.resetQueue()
    Thread.sleep(2000) // To get rid of request in the queues thread

    pool.stopServer()

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
      val response = controller.proxyPass.apply(FakeRequest(GET, "/test/proxy").withHeaders(("Content-Type", "")).withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

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
      val response = controller.proxyPass.apply(FakeRequest(POST, "/test/proxy").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must include ("{\"success\": true}")
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
     * * proxy status is healthy
     */
    "return 200 on reloading config" in {
      val bytes: ByteString = ByteString("")
      val fakeRequest = FakeRequest(POST, "/config/reload").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.reloadConfig.apply(fakeRequest)

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      while (!ProxyStatus.isHealthy) Thread.sleep(1000)
      ProxyStatus.isHealthy mustBe true
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
      val body: String =
        """
          |{
          |  "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
          |  "w": "An_Invalid_w",
          |  "n": "An_Invalid_n",
          |  "d": 4196585670338033714759641235444284559441802073000000000000000000000000000000
          |}
          |""".stripMargin
      val bytes: ByteString = ByteString(body)
      val fakeRequest = FakeRequest(POST, "/mining/solution").withHeaders("api_key" -> "some string", "Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.sendSolution.action.apply(fakeRequest)

      status(response) mustBe BAD_REQUEST
      contentType(response) mustBe Some("application/json")
      PoolShareQueue.length mustBe 0
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
      PoolShareQueue.resetQueue()

      val body: String =
        """
          |{
          |  "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
          |  "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
          |  "n": "0000000010C006CF",
          |  "d": 4196585670338033714759641235444284559441802073009721710293850518130743229130
          |}
          |""".stripMargin
      val bytes: ByteString = ByteString(body)
      val fakeRequest = FakeRequest(POST, "/mining/solution").withHeaders("api_key" -> "some string", "Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.sendSolution.action.apply(fakeRequest)

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must include ("{\"success\": true}")
      PoolShareQueue.length mustBe 1
    }
  }

  /** Check get candidate requests */
  "ProxyController getMiningCandidate" should {
    /**
     * Purpose: Check mining would be disabled when there is not enough boxes
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake GET request to `/mining/candidate` and passes it to the app.
     *           Then as it's a new block header and proof is null but there is not enough boxes, it will throw
     *           exception and disable mining
     * Test Conditions:
     * * exception is thrown
     */
    "throw exception when there is not enough boxes" in {
      PoolShareQueue.resetQueue()

      val msg: String = "First_msg"
      NodeServlets.msg = msg
      Config.blockHeader = ""

      NodeServlets.proof = "null"

      NodeServlets.unspentBoxes =
        s"""
           |[
           |  {
           |    "box": {
           |      "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
           |      "value": 60500000000
           |    },
           |    "address": "${NodeServlets.protectionAddress}"
           |  },
           |  {
           |    "box": {
           |      "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
           |      "value": 147
           |    },
           |    "address": "another_address"
           |  }
           |]
           |""".stripMargin
      Node.createProtectionScript()
      Node.fetchUnspentBoxes()
      val bytes: ByteString = ByteString("")
      val triedStatement = Try {
        controller.getMiningCandidate.action.apply(FakeRequest(GET, "/mining/candidate").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))
      }
      ProxyStatus.reset()
      triedStatement match {
        case Failure(_) =>
        case _ =>
          fail("Expected to throw exception but didn't")
      }
    }

    /**
     * Purpose: Check proof will be created when it's null and there is enough boxes
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake GET request to `/mining/candidate` and passes it to the app.
     *           Then as it's a new block header and proof is null, `/wallet/transaction/generate` and `/mining/candidateWithTxs` should being called.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is equal to bodyCheck variable
     * * proof of TestNode must not be equal to "null" string
     */
    "return 200 status code with new header and generate proof when there is enough boxes" in {
      PoolShareQueue.resetQueue()

      val msg: String = "First_msg"
      NodeServlets.msg = msg

      NodeServlets.proof mustBe "null"

      NodeServlets.unspentBoxes =
        s"""
           |[
           |  {
           |    "box": {
           |      "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
           |      "value": 60500000000
           |    },
           |    "address": "${NodeServlets.protectionAddress}"
           |  },
           |  {
           |    "box": {
           |      "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
           |      "value": 7501000000
           |    },
           |    "address": "${NodeServlets.protectionAddress}"
           |  },
           |  {
           |    "box": {
           |      "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
           |      "value": 147
           |    },
           |    "address": "another_address"
           |  }
           |]
           |""".stripMargin
      Node.createProtectionScript()
      Node.fetchUnspentBoxes()
      val bytes: ByteString = ByteString("")
      val response = controller.getMiningCandidate.action.apply(FakeRequest(GET, "/mining/candidate").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      val bodyCheck: String =
        s"""
          |{
          |  "msg": "$msg",
          |  "b": 748014723576678314041035877227113663879264849498014394977645987,
          |  "pk": "0278011ec0cf5feb92d61adb51dcb75876627ace6fd9446ab4cabc5313ab7b39a7",
          |  "pb": 7480147235766783140410358772271136638792648494980143949776459870
          |}
          |""".stripMargin.replaceAll("\\s", "")

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response).replaceAll("\\s", "") must include (bodyCheck)
      PoolServerServlets.gotProof mustBe true
      NodeServlets.proof must not be "null"
    }

    /**
     * Purpose: Check proof won't be sent to the pool server if message didn't change
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake GET request to `/mining/candidate` and passes it to the app.
     *           Then as it's the same block header, proof won't be created and it won't be sent to the pool server.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is equal to bodyCheck variable
     * * gotProof of the pool server must be false
     * * proofCreated of the node must be false
     */
    "return 200 status code with same header" in {
      PoolShareQueue.resetQueue()

      val msg: String = "First_msg"
      NodeServlets.msg = msg
      PoolServerServlets.gotProof = false
      NodeServlets.proofCreated = false

      val bytes: ByteString = ByteString("")
      val response = controller.getMiningCandidate.action.apply(FakeRequest(GET, "/mining/candidate").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      val bodyCheck: String =
        s"""
          |{
          |  "msg": "$msg",
          |  "b": 748014723576678314041035877227113663879264849498014394977645987,
          |  "pk": "0278011ec0cf5feb92d61adb51dcb75876627ace6fd9446ab4cabc5313ab7b39a7",
          |  "pb": 7480147235766783140410358772271136638792648494980143949776459870
          |}
          |""".stripMargin.replaceAll("\\s", "")

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response).replaceAll("\\s", "") must include (bodyCheck)
      PoolServerServlets.gotProof mustBe false
      NodeServlets.proofCreated mustBe false
    }

    /**
     * Purpose: Check existing proof will be sent to the pool server and it would not be created again
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: It sends a fake GET request to `/mining/candidate` and passes it to the app.
     *           Then as it's a new block header but proof exists, the proof will be sent to the pool server without being created again.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is equal to bodyCheck variable
     * * proof of TestNode must not be equal to "null" string
     * * gotProof of the pool server must be true
     * * proofCreated of the node must be false
     */
    "return 200 status code with new header but existing proof" in {
      PoolShareQueue.resetQueue()

      val msg: String = "Second_msg"
      NodeServlets.msg = msg

      NodeServlets.proofCreated = false

      val bytes: ByteString = ByteString("")
      val response = controller.getMiningCandidate.action.apply(FakeRequest(GET, "/mining/candidate").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      val bodyCheck: String =
        s"""
          |{
          |  "msg": "$msg",
          |  "b": 748014723576678314041035877227113663879264849498014394977645987,
          |  "pk": "0278011ec0cf5feb92d61adb51dcb75876627ace6fd9446ab4cabc5313ab7b39a7",
          |  "pb": 7480147235766783140410358772271136638792648494980143949776459870
          |}
          |""".stripMargin.replaceAll("\\s", "")

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response).replaceAll("\\s", "") must include (bodyCheck)
      PoolServerServlets.gotProof mustBe true
      NodeServlets.proofCreated mustBe false
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
      PoolShareQueue.resetQueue()

      val body: String =
        """
          |{
          |  "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
          |  "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
          |  "n": "0000000010C006CF",
          |  "d": 4196585670338033714759641235444284559441802073009721710293850518130743229130
          |}
          |""".stripMargin
      val bytes: ByteString = ByteString(body)
      val fakeRequest = FakeRequest(POST, "/mining/share").withHeaders("api_key" -> "some string", "Content_type" -> "application/json").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = controller.sendShare.action.apply(fakeRequest)

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response).replaceAll("\\s", "") must include ("{}")
      PoolShareQueue.length mustBe 1
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

  /** Check Info */
  "ProxyController changeInfo" should {
    val greenInfo: String =
      """
        |{
        |  "proxy" : {
        |    "pool" : {
        |      "connection" : "http://localhost:9001",
        |      "config" : {
        |        "wallet" : "3WvrVTCPJ1keSdtqNL5ayzQ62MmTNz4Rxq7vsjcXgLJBwZkvHrGa",
        |        "difficulty_factor" : 10.0,
        |        "transaction_request_value" : 67500000000
        |      }
        |    },
        |    "status" : {
        |      "health" : "GREEN"
        |    }
        |  }
        |}
        |""".stripMargin.replaceAll("\\s", "")

    val redInfo: String =
      """
        |{
        |  "proxy" : {
        |    "pool" : {
        |      "connection" : "http://localhost:9001",
        |      "config" : {
        |        "wallet" : "3WvrVTCPJ1keSdtqNL5ayzQ62MmTNz4Rxq7vsjcXgLJBwZkvHrGa",
        |        "difficulty_factor" : 10.0,
        |        "transaction_request_value" : 67500000000
        |      }
        |    },
        |    "status" : {
        |      "health" : "RED",
        |      "reason": "[Test] this is test"
        |    }
        |  }
        |}
        |""".stripMargin.replaceAll("\\s", "")
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
      val bytes: ByteString = ByteString("")
      val response = controller.changeInfo.apply(FakeRequest(GET, "/info").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response).replaceAll("\\s", "") mustBe greenInfo
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
      ProxyStatus.setStatus(StatusType.red, "Test", "this is test")
      val bytes: ByteString = ByteString("")
      val response = controller.changeInfo.apply(FakeRequest(GET, "/info").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response).replaceAll("\\s", "") mustBe redInfo
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
        |   "message": "This is test"
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
      ProxyStatus.setStatus(StatusType.red, "Something", "This is test")
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
      ProxyStatus.setStatus(StatusType.red, "Config", "This is test")
      val bytes: ByteString = ByteString("")
      val response = controller.resetStatus.apply(FakeRequest(POST, "/status/reset").withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes)))

      status(response) mustBe INTERNAL_SERVER_ERROR
      contentType(response) mustBe Some("application/json")
      contentAsString(response).replaceAll("\\s", "") mustBe failed
    }
  }
}
