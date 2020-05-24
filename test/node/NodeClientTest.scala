package node

import java.io.File

import helpers.{ConfigTrait, Helper}
import models.{Block, BlockFinder, Box, BoxFinder}
import org.ergoplatform.appkit.NetworkType
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.db.DBApi
import play.api.db.evolutions.Evolutions
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import proxy.Mnemonic
import testservers.{NodeServlets, TestNode}

class NodeClientTest extends PlaySpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach with ConfigTrait {

  var node = new TestNode(9001)

  /**
   * Apply migrations to database before running units
   */
  override def beforeAll(): Unit = {
    node.startServer()
    super.beforeAll()
  }

  /**
   * Remove database after all units
   */
  override def afterAll(): Unit = {
    node.stopServer()
    super.afterAll()
  }

  "NodeClient" should {
    /**
     * Purpose: check validity of miner address
     * Prerequisites: None.
     */
    "have valid miner address" in {
      val node = new NodeClient
      NodeServlets.walletAddresses = Vector[String]("3WyhTEvTPUdEKutmcRZhcFb7Akmpv1EqN9UEUiSmkb74vTW9Xste", "3Wx95JCL6AQS17YtkTfRFJ98UiACimZcHLYpBFLGP53NzsTsxjvk")
      node.minerAddress mustBe "3WyhTEvTPUdEKutmcRZhcFb7Akmpv1EqN9UEUiSmkb74vTW9Xste"
    }

    /**
     * Purpose: check validity of pk
     * Prerequisites: None.
     * Scenario: get pk
     * Test Conditions:
     *  pk must be equal to the one provided in mining candidate result
     */
    "have valid pk" in {
      val node = new NodeClient
      NodeServlets.walletAddresses = Vector[String]("3WyhTEvTPUdEKutmcRZhcFb7Akmpv1EqN9UEUiSmkb74vTW9Xste", "3Wx95JCL6AQS17YtkTfRFJ98UiACimZcHLYpBFLGP53NzsTsxjvk")
      NodeServlets.miningCandidate =
        """
          |{
          |  "msg": "fadb24a6cb2cc09107d867cf04a8516fa8bfcfee96230cb6ec22375702e43b07",
          |  "b": 489744973727878998844494729240144523534431382483931218168402860391,
          |  "pk": "0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |  "proof": null
          |}
          |""".stripMargin
      node.pk mustBe "0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435"
    }

    /**
     * Purpose: check validity network type
     * Prerequisites: None.
     * Scenario: get network type
     * Test Conditions:
     *  network type must be TESTNET because of the wallet address
     */
    "have network type of TestNet" in {
      val node = new NodeClient
      NodeServlets.walletAddresses = Vector[String]("3WyhTEvTPUdEKutmcRZhcFb7Akmpv1EqN9UEUiSmkb74vTW9Xste", "3Wx95JCL6AQS17YtkTfRFJ98UiACimZcHLYpBFLGP53NzsTsxjvk")
      node.networkType mustBe NetworkType.TESTNET
    }

    /**
     * Purpose: check validity network type
     * Prerequisites: None.
     * Scenario: get network type
     * Test Conditions:
     *  network type must be MAINNET because of the wallet address
     */
    "have network type of MainNet" in {
      val node = new NodeClient
      NodeServlets.walletAddresses = Vector[String]("9iHWcYYSPkgYbnC6aHfZcLZrKrrkpFzM2ETUZ2ikFqFwVAB2CU7")
      node.networkType mustBe NetworkType.MAINNET
    }

    /**
     * Purpose: check validity of isTransactionMined method
     * Prerequisites: None.
     * Scenario: check one mined transaction and one which isn't mined yet
     * Test Conditions:
     *   return true for mined one and false for the other
     */
    "return valid in isTransactionMined" in {
      val node = new NodeClient
      NodeServlets.failTransaction = false
      var res = node.isTransactionMined("2ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117")
      res mustBe true

      NodeServlets.failTransaction = true
      res = node.isTransactionMined("2ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117")
      res mustBe false
    }

    /**
     * Purpose: check validity of boxExists method
     * Prerequisites: None.
     * Scenario: check to see if box exists for two boxes
     * Test Conditions:
     *   return true for first one and false for the other
     */
    "return valid in isBoxExists" in {
      val node = new NodeClient
      NodeServlets.failTransaction = false
      var res = node.isBoxExists("id")
      res mustBe true

      NodeServlets.failTransaction = true
      res = node.isBoxExists("otherId")
      res mustBe false
    }
  }
}