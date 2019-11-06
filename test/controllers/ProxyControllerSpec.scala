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
  "ProxyController GET" should {

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
      val proxy = controller.proxy().apply(FakeRequest(GET, testRoute).withHeaders("api_key" -> "some string"))

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
      val proxy = controller.proxy().apply(FakeRequest(GET, testRoute).withHeaders("api_key" -> "some string"))

      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"success\": true}")
    }
  }

  /** Check GET requests */
  "ProxyController POST" should {

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
      val proxy = controller.proxy().apply(fakeRequest)

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
      val proxy = controller.proxy().apply(fakeRequest)

      status(proxy) mustBe OK
      contentType(proxy) mustBe Some("application/json")
      contentAsString(proxy) must include ("{\"success\": true}")
    }
  }
}
