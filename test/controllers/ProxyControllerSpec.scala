package controllers

import controllers.testservers.{TestNode, TestPoolServer}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.BeforeAndAfterAll

/** Check if proxy server would pass any POST or GET requests with their header and body with any route to that route of the specified node */ 
class ProxyControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterAll {
  val testNodeConnection: String = "http://localhost:9001"
  val testPoolServerConnection: String = "http://localhost:9002"
  val node: TestNode.type = TestNode
  val pool: TestPoolServer.type = TestPoolServer

  override def beforeAll(): Unit = {
    node.startServer()
    pool.startServer()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    node.stopServer()
    pool.stopServer()
    super.afterAll()
  }

  /** Check ordinary routes */
  "ProxyController proxyPass Ordinary Routes" should {

    /** 
     * Purpose: Check if proxy works on GET requests for ordinary routes.
     * Prerequisites: Check test node and test pool server connections in test.conf .
     * Scenario: It sends a fake GET request to `/test/proxy` of the app and checks if response is OK.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return success for a get request" in {
      val response = route(app, FakeRequest(GET, "/test/proxy")).get

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must include ("{\"success\": true}")
    }

    /**
     * Purpose: Check if proxy works on POST requests for ordinary routes.
     * Prerequisites: Check test node and test pool server connections in test.conf .
     * Scenario: It sends a fake POST request to `/test/proxy` of the app and checks if response is OK.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return success for a post request" in {
      val response = route(app, FakeRequest(POST, "/test/proxy")).get

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must include ("{\"success\": true}")
    }
  }

  /** Check solution requests */
  "ProxyController solution" should {
    /**
     * Purpose: Check solution won't be sent to pool server if status is 400.
     * Prerequisites: Check test node and test pool server connections in test.conf .
     * Scenario: It sends a fake POST request with an invalid body to `/mining/solution` to the app.
     *           As the solution is invalid, status would be 400 so it won't send the request to the pool server.
     * Test Conditions:
     * * TestPoolServer.PoolServlets.gotSolution is false
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
      val fakeRequest = FakeRequest(POST, "/mining/solution").withHeaders("api_key" -> "some string", "Content_type" -> "application/json").withBody[String](body)
      val response = route(app, fakeRequest).get

      status(response) mustBe BAD_REQUEST
      contentType(response) mustBe Some("application/json")
      pool.Servlets.gotSolution mustBe false
    }


    /**
     * Purpose: Check solution will be sent to pool server if status is 200.
     * Prerequisites: Check test node and test pool server connections in test.conf .
     * Scenario: It sends a fake POST request to `/mining/solution` to the app.
     *           Status is 200 so it should send the request to the pool server.
     * Test Conditions:
     * * TestPoolServer.PoolServlets.gotSolution is true
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return 200 status code on correct solution" in {
      val body: String =
        """
          |{
          |  "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
          |  "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
          |  "n": "0000000010C006CF",
          |  "d": 4196585670338033714759641235444284559441802073009721710293850518130743229130
          |}
          |""".stripMargin
      val fakeRequest = FakeRequest(POST, "/mining/solution").withHeaders("api_key" -> "some string", "Content_type" -> "application/json").withBody[String](body)
      val response = route(app, fakeRequest).get

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must include ("{\"success\": true}")
      pool.Servlets.gotSolution mustBe true
    }
  }

  /** Check get candidate requests */
  "ProxyController getMiningCandidate" should {

    /**
     * Purpose: Check proof will be created when it's null
     * Prerequisites: Check test node and test pool server connections in test.conf .
     * Scenario: It sends a fake GET request to `/mining/candidate` and passes it to the app.
     *           Then as it's a new block header and proof is null, `/wallet/transaction/generate` and `/mining/candidateWithTxs` should being called.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is equal to bodyCheck variable
     * * proof of TestNode must not be equal to "null" string
     */
    "return 200 status code with new header and generate proof" in {
      val msg: String = "First_msg"
      node.Servlets.msg = msg
      node.Servlets.proof mustBe "null"
      val response = route(app, FakeRequest(GET, "/mining/candidate")).get

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
      pool.Servlets.gotProof mustBe true
      node.Servlets.proof must not be "null"
    }

    /**
     * Purpose: Check proof won't be sent to the pool server if message didn't change
     * Prerequisites: Check test node and test pool server connections in test.conf .
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
      val msg: String = "First_msg"
      node.Servlets.msg = msg
      pool.Servlets.gotProof = false
      node.Servlets.proofCreated = false
      val response = route(app, FakeRequest(GET, "/mining/candidate")).get

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
      pool.Servlets.gotProof mustBe false
      node.Servlets.proofCreated mustBe false
    }

    /**
     * Purpose: Check existing proof will be sent to the pool server and it would not be created again
     * Prerequisites: Check test node and test pool server connections in test.conf .
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
      val msg: String = "Second_msg"
      node.Servlets.msg = msg
      node.Servlets.proofCreated = false
      val response = route(app, FakeRequest(GET, "/mining/candidate")).get

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
      pool.Servlets.gotProof mustBe true
      node.Servlets.proofCreated mustBe false
    }
  }

  /** Check share requests */
  "ProxyController sendShare" should {

    /**
     * Purpose: Check if share will be sent to the pool server.
     * Prerequisites: Check test node and test pool server connections in test.conf .
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
      val fakeRequest = FakeRequest(POST, "/mining/solution").withHeaders("api_key" -> "some string", "Content_type" -> "application/json").withBody[String](body)
      val response = route(app, fakeRequest).get

      status(response) mustBe OK
      contentType(response) mustBe Some("application/json")
      contentAsString(response) must include ("{\"success\": true}")
    }
  }
}
