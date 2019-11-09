package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.libs.json.Json
import play.api.Logger
import loggers.ServerLogger

/** Check if proxy server would pass any POST or GET requests with their header and body with any route to that route of the specified node */ 
class ProxyControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  val logger = new ServerLogger
  val config = ConfigFactory.load("test.conf")
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
      val testRoute = config.getString("test.route1")
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
      val testRoute = config.getString("test.route2")
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
      val testRoute = config.getString("test.route1")
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
      val testRoute = config.getString("test.route2")
      val fakeRequest = FakeRequest(POST, testRoute).withHeaders("api_key" -> "some string").withJsonBody(Json.obj("key" -> "value"))
      val proxy = controller.proxyPass().apply(fakeRequest)

      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"success\": true}")
    }
  }

  /** Check solution requests */
  "ProxyController solution" should {

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
      val testRoute = config.getString("test.route1")
      val fakeRequest = FakeRequest(POST, testRoute).withHeaders("api_key" -> "some string").withJsonBody(Json.obj("key" -> "value"))
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
      val testRoute = config.getString("test.route400")
      val fakeRequest = FakeRequest(POST, testRoute).withHeaders("api_key" -> "some string").withJsonBody(Json.obj("key" -> "value"))
      val proxy = controller.solution().apply(fakeRequest)

      status(proxy) mustBe 400
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"success\": false}")
    }
  }

  /** Check solution requests */
  "ProxyController getMiningCandidate" should {

    /** 
     * Purpose: Check pb key is in the response.  
     * Prerequisites: Check if mock api (configured in test.conf) works and change it if wanted.  
     * Scenario: It gets config from `test.conf` and sends a fake GET request to `test.route1` and passes it with the config to ProxyController. 
     *           Then returns the response with pb in the body.
     * Test Conditions:
     * * status is `200`
     * * Content-Type is `application/json`
     * * Content is `{"pb":7.991447316561134E+63,"success":true}`
     */
    "return 200 status code from a new instance of controller" in {
      val testRoute = config.getString("test.route1")
      val proxy = controller.getMiningCandidate().apply(FakeRequest(GET, testRoute).withHeaders("api_key" -> "some string"))
      val pb: String = Json.toJson(config.getDouble("pool.server.difficulty")).toString

      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"pb\":" + pb + ",\"success\":true}")
    }
  }
}
