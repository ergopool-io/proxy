package node

import helpers.{ConfigTrait, Helper}
import io.circe.Json
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.db.DBApi
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

class ShareTest extends PlaySpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach with ConfigTrait {

  "Share" should {
    /**
     * Purpose: single valid share
     * Prerequisites: None.
     * Scenario: create share with one valid share body
     * Test Conditions:
     *   must return vector with the one valid share
     */
    "return single share with one requested share" in {
      val shareBody =
        """
          |{
          |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |      "w": "0204787c4a256cb31f0e1c3c0a65907cddf7fa64b50c29ec90d2f2e9311315d3c0",
          |      "n": "000013c2df7944ce",
          |      "d": 126332151464134121647421875202846731543931375566395403436590117338
          |}
          |""".stripMargin
      val share = Share(Helper.convertToJson(shareBody))
      share.length mustBe 1
      share.apply(0).body mustBe Helper.convertToJson(
        """
          |{
          |    "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |    "w": "0204787c4a256cb31f0e1c3c0a65907cddf7fa64b50c29ec90d2f2e9311315d3c0",
          |    "nonce": "000013c2df7944ce",
          |    "d": "126332151464134121647421875202846731543931375566395403436590117338"
          |}
          |""".stripMargin
      )
    }

    /**
     * Purpose: single valid share
     * Prerequisites: None.
     * Scenario: create 3 valid shares
     * Test Conditions:
     *   must return vector with the three one valid share
     */
    "return vector of valid shares" in {
      val shareBody =
        """
          |[
          |    {
          |        "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |        "w": "0204787c4a256cb31f0e1c3c0a65907cddf7fa64b50c29ec90d2f2e9311315d3c0",
          |        "n": "000013c2df7944ce",
          |        "d": 126332151464134121647421875202846731543931375566395403436590117338
          |    },
          |    {
          |        "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |        "w": "0280937a35bae88fd6ad8fe87bc87ab6d6c307de7b78976691572d097da0f61255",
          |        "n": "000013fd9311bec1",
          |        "d": 126332151464134121647421875202846731543931375566395403436590117338
          |    },
          |    {
          |        "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |        "w": "029d706741d4b411f78acf34273761cd324bf97d475c5cde8d455938062b841fb7",
          |        "n": "0000141e0cfef4f1",
          |        "d": 126332151464134121647421875202846731543931375566395403436590117338
          |    }
          |]
          |""".stripMargin
      val share = Share(Helper.convertToJson(shareBody))
      share.length mustBe 3
      share.map(_.body) mustBe Helper.convertToJson("""
          |[
          |    {
          |        "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |        "w": "0204787c4a256cb31f0e1c3c0a65907cddf7fa64b50c29ec90d2f2e9311315d3c0",
          |        "nonce": "000013c2df7944ce",
          |        "d": "126332151464134121647421875202846731543931375566395403436590117338"
          |    },
          |    {
          |        "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |        "w": "0280937a35bae88fd6ad8fe87bc87ab6d6c307de7b78976691572d097da0f61255",
          |        "nonce": "000013fd9311bec1",
          |        "d": "126332151464134121647421875202846731543931375566395403436590117338"
          |    },
          |    {
          |        "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |        "w": "029d706741d4b411f78acf34273761cd324bf97d475c5cde8d455938062b841fb7",
          |        "nonce": "0000141e0cfef4f1",
          |        "d": "126332151464134121647421875202846731543931375566395403436590117338"
          |    }
          |]
          |""".stripMargin).asArray.getOrElse(Vector[Json]())
    }
  }
}
