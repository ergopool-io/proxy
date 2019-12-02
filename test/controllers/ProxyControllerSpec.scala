package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import com.typesafe.config.{Config, ConfigFactory}
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import loggers.ServerLogger

/** Check if proxy server would pass any POST or GET requests with their header and body with any route to that route of the specified node */ 
class ProxyControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  val logger = new ServerLogger
  val config: Config = ConfigFactory.load("test.conf")
  val controller = new ProxyController(stubControllerComponents())(Configuration(config))(logger)
  
  /** Check GET requests */
  "ProxyController proxyPass GET" should {

    /** 
     * Purpose: Check if proxy works on GET requests.  
     * Prerequisites: Check if mock api (configured in test.conf) works and change it if wanted.  
     * Scenario: It gets config from `test.conf` and sends a fake GET request to `test.route1` passes it with the config to ProxyController.  
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return success from a new instance of controller for first route" in {
      val testRoute = config.getString("test.route.1")
      val proxy = controller.proxyPass().apply(FakeRequest(GET, testRoute).withHeaders("api_key" -> "some string"))

      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"success\": true}")
    }

    /** 
     * Purpose: Check if proxy works with any route on GET method. 
     * Prerequisites: Check if mock api (configured in test.conf) works and change it if wanted.  
     * Scenario: It gets config from `test.conf` and sends a fake GET request to `test.route1` passes it with the config to ProxyController.  
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return success from a new instance of controller for second route" in {
      val testRoute = config.getString("test.route.2")
      val proxy = controller.proxyPass().apply(FakeRequest(GET, testRoute).withHeaders("api_key" -> "some string"))

      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"success\": true}")
    }
  }

  /** Check POST requests */
  "ProxyController proxyPass POST" should {

    /** 
     * Purpose: Check if proxy works on POST requests.  
     * Prerequisites: Check if mock api (configured in test.conf) works and change it if wanted.  
     * Scenario: It gets config from `test.conf` and sends a fake GET request to `test.route1` passes it with the config to ProxyController.  
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return success from a new instance of controller for first route" in {
      val testRoute = config.getString("test.route.1")
      val fakeRequest = FakeRequest(POST, testRoute).withHeaders("api_key" -> "some string").withJsonBody(Json.obj("key" -> "value"))
      val proxy = controller.proxyPass().apply(fakeRequest)

      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"success\": true}")
    }

    /** 
     * Purpose: Check if proxy works with any route on POST method.  
     * Prerequisites: Check if mock api (configured in test.conf) works and change it if wanted.  
     * Scenario: It gets config from `test.conf` and sends a fake GET request to `test.route1` passes it with the config to ProxyController.  
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return success from a new instance of controller for second route" in {
      val testRoute = config.getString("test.route.2")
      val fakeRequest = FakeRequest(POST, testRoute).withHeaders("api_key" -> "some string").withJsonBody(Json.obj("key" -> "value"))
      val proxy = controller.proxyPass().apply(fakeRequest)

      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"success\": true}")
    }
  }

  /** Check solution requests */
  "ProxyController solution" should {
    val body: JsObject = Json.obj(
      "pk" -> "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
      "w" -> "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
      "n" -> "0000000000000000",
      "d" -> BigInt("799144731656113400000000000000000000000000000000000000000000000")
    )
    /** 
     * Purpose: Check solution function will send request to pool server if status is 200.  
     * Prerequisites: Check if mock api (configured in test.conf) works and change it if wanted.  
     * Scenario: It gets config from `test.conf` and sends a fake POST request to `test.route1` passes it with the config to ProxyController.
     *           Status is 200 so it should send the request to the pool server.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success": true}`
     */
    "return 200 status code from a new instance of controller" in {
      val testRoute = config.getString("test.route.1")
      val fakeRequest = FakeRequest(POST, testRoute).withHeaders("api_key" -> "some string").withJsonBody(body)
      val proxy = controller.solution().apply(fakeRequest)

      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"success\": true}")
    }

    /** 
     * Purpose: Check solution function won't send request to pool server if status is 400. 
     * Prerequisites: Check if mock api (configured in test.conf) works and change it if wanted.  
     * Scenario: It gets config from `test.conf` and sends a fake POST request to `test.route400` passes it with the config to ProxyController. 
     *           Status is 400 so it shouldn't send the request to the pool server.  
     * Test Conditions:
     * * status is `400`
     * * Content-Type is `application/json`
     * * Content is `{"success": false}`
     */
    "return 400 status code from a new instance of controller" in {
      val testRoute = config.getString("test.route.400")
      val fakeRequest = FakeRequest(POST, testRoute).withHeaders("api_key" -> "some string").withJsonBody(body)
      val proxy = controller.solution().apply(fakeRequest)

      status(proxy) mustBe 400
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"success\": false}")
    }
  }

  /** Check get candidate requests */
  "ProxyController getMiningCandidate" should {

    val testRoute = config.getString("test.route.candidate")
    val pb: String = ConfigFactory.load().getString("pool.difficulty")
    var blockHeader: String = ""

    /**
     * Purpose: Check pb key is in the response and the block header is set.
     * Prerequisites: Check if mock api (configured in test.conf) works and change it if wanted.  
     * Scenario: It gets config from `test.conf` and sends a fake GET request to `test.route1` and passes it with the config to ProxyController. 
     *           Then as it's a new block header, blockHeader of controller should change from empty to sth and then it should return the response with pb in the body.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"pb":799144731656113400000000000000000000000000000000000000000000000,"success":true,"msg":"0350e25cee856...","b":987654321,"pk":"0350e25cee85..."}`
     */
    "return 200 status code from a new instance of controller for a new block header" in {
      val proxy = controller.getMiningCandidate().apply(FakeRequest(GET, testRoute).withHeaders("api_key" -> "some string"))
      blockHeader = controller.blockHeader

      blockHeader must not be empty
      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy).replaceAll("\\s", "") must include ("{\"msg\":\"0350e25cee856...\",\"b\":987654321,\"pk\":\"0350e25cee85...\",\"pb\":" + pb + "}")
    }

    /**
     * Purpose: Check pb key is in the response and the block header didn't change as it is the same as last request.
     * Prerequisites: Check if mock api (configured in test.conf) works and change it if wanted.
     * Scenario: It gets config from `test.conf` and sends a fake GET request to `test.route1` and passes it with the config to ProxyController.
     *           Then returns the response with pb in the body and the blockHeader should remain as it is because the block header didn't change.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"pb":799144731656113400000000000000000000000000000000000000000000000,"success":true,"msg":"0350e25cee856...","b":987654321,"pk":"0350e25cee85..."}`
     */
    "return 200 status code from a new instance of controller for the same block header" in {
      val proxy = controller.getMiningCandidate().apply(FakeRequest(GET, testRoute).withHeaders("api_key" -> "some string"))

      controller.blockHeader must equal (blockHeader)
      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy).replaceAll("\\s", "") must include ("{\"msg\":\"0350e25cee856...\",\"b\":987654321,\"pk\":\"0350e25cee85...\",\"pb\":" + pb + "}")
    }
  }

  /** Check share requests */
  "ProxyController sendShare" should {

    /**
     * Purpose: Check if shared would be sent to the pool server.
     * Prerequisites: Check if mock api (configured in test.conf) works and change it if wanted.
     * Scenario: It gets config from `test.conf` and sends a fake GET request to `test.route1` passes it with the config to ProxyController.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"success":true}`
     */
    "return 200 status code from a new instance of controller" in {
      val testRoute = config.getString("test.route.1")
      val proxy = controller.sendShare().apply(FakeRequest(POST, testRoute).withHeaders("api_key" -> "some string"))

      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy).replaceAll("\\s", "") must include ("{}")
    }
  }
}
