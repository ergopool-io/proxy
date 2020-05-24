package node

import java.io.File

import helpers.{ConfigTrait, Helper}
import models.{Block, BlockFinder, Box, BoxFinder}
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

class TransactionHandlerTest extends PlaySpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach with ConfigTrait {

  lazy val appBuilder = new GuiceApplicationBuilder()
  lazy val injector: Injector = appBuilder.injector()
  lazy val databaseApi: DBApi = injector.instanceOf[DBApi]

  val nodeConfig: NodeConfig = mock[NodeConfig]
  val client: NodeClient = new NodeClient
  val testNodeConnection: String = readKey("node.connection")
  NodeServlets.walletAddresses = Vector[String]("3WyhTEvTPUdEKutmcRZhcFb7Akmpv1EqN9UEUiSmkb74vTW9Xste")
  NodeServlets.failTransaction = false
  var node = new TestNode(9001)
  val testMnemonic: Mnemonic = mock[Mnemonic]
  when(testMnemonic.value).thenReturn("census erode want novel panther lens head hire else south leaf situate hill tiger metal maze amused short repeat also canvas skate outdoor video")

  val minerAddress = "3WyhTEvTPUdEKutmcRZhcFb7Akmpv1EqN9UEUiSmkb74vTW9Xste"
  val lockAddress = "3WyRt8MCd1XfZnWoPXdrngt9yb2BgdxRtM8RQz7btQSB32XF7sVf"
  val withdrawAddress = "3WxHMNzzaBXLS2xiPPeK61PhM1h9yr8nQrpZHH5nnJJ2hWDoAoqw"

  /**
   * Apply migrations to database before running units
   */
  override def beforeAll(): Unit = {
    Evolutions.applyEvolutions(databaseApi.database("default"))
    node.startServer()

    super.beforeAll()
  }

  /**
   * Remove database after all units
   */
  override def afterAll(): Unit = {
    Evolutions.cleanupEvolutions(databaseApi.database("default"))
    new File("test.db").delete()
    node.stopServer()

    super.afterAll()
  }

  /**
   * Remove all data after each unit
   */
  override protected def afterEach(): Unit = {
    new BoxFinder().query().delete()
    new BlockFinder().query().delete()
    super.afterEach()
  }

  "TransactionHandler" should {
    /**
     * Purpose: return null if not enough boxes are available.
     * Prerequisites: None.
     * Test Conditions:
     * * There is 0 boxes in DB
     */
    "return null custom transaction" in {
      val txHandler = new TransactionHandler(client, lockAddress, withdrawAddress, testMnemonic)
      val value = 65000000000L

      val customTx = txHandler.getCustomTransaction(value)
      customTx mustBe null
    }

    /**
     * Purpose: return null if not enough boxes are available.
     * Prerequisites: None.
     * Test Conditions:
     * * There is 1 boxe in DB but it does not have required address
     */
    "return null pool transaction" in {
      val txHandler = new TransactionHandler(client, lockAddress, withdrawAddress, testMnemonic)
      val value = 65000000000L
      val boxJs =
        """
          |{
          |      "boxId": "box1",
          |      "value": 67500000000,
          |      "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |      "assets": [],
          |      "creationHeight": 464846,
          |      "additionalRegisters": {},
          |      "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
          |      "index": 2
          |}
          |""".stripMargin
      val box = Box(Helper.convertToJson(boxJs), client.networkType, 0)
      box.createdIn = Block("forB1", 1000)
      box.save()

      val customTx = txHandler.getPoolTransaction(minerAddress, value)
      customTx mustBe null
    }

    /**
     * Purpose: return valid custom transaction.
     * Prerequisites: None.
     * Test Conditions:
     * * There is 1 box in DB
     */
    "return valid custom transaction" in {
      val txHandler = new TransactionHandler(client, lockAddress, withdrawAddress, testMnemonic)
      val value = 65000000000L
      val boxJs =
        """
          |{
          |      "boxId": "box1",
          |      "value": 67500000000,
          |      "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |      "assets": [],
          |      "creationHeight": 464846,
          |      "additionalRegisters": {},
          |      "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
          |      "index": 2
          |}
          |""".stripMargin
      val box = Box(Helper.convertToJson(boxJs), client.networkType, 0)
      box.createdIn = Block("forB1", 1000)
      box.save()

      val customTx = txHandler.getCustomTransaction(value)
      customTx mustBe a [Transaction]
    }

    /**
     * Purpose: return valid pool transaction.
     * Prerequisites: None.
     * Test Conditions:
     * * There is 1 box in DB
     */
    "return valid pool transaction" in {
      val txHandler = new TransactionHandler(client, lockAddress, withdrawAddress, testMnemonic)
      NodeServlets.protectionAddress = ""
      val value = 65000000000L
      val boxJs =
        """
          |{
          |      "boxId": "5765a3352e8d2dcd6ad72870756ffb1de899178f76c893f14d5ce9f5f064ea6c",
          |      "value": 175000000000,
          |      "ergoTree": "100308cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc43508cd0373a3b15ecbe62e0bb94e05c6271de5bba07ef6435294157e585821cd6b0dd55208cd02dc931b18eaedcf92fb70dcb9216ede1f9d430f9ae1f7f436b81a186f2587cf51eb02ea02d193d0cddb6906db6503fed0730073017302",
          |      "assets": [],
          |      "creationHeight": 115596,
          |      "additionalRegisters": {},
          |      "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
          |      "index": 0
          |}
          |""".stripMargin
      val box = Box(Helper.convertToJson(boxJs), "5Hg4a36kQGxB6zki3oCYbSfL3yfmhGCc4yYfRUwEdgqMWPbY57TAZozkvVunEsUmb5vgRU6iiFQzJkpaR8PJ7hfzbj8NNTf5Rmf2ou9LrmafJDD1ewX9YjqgGHWgJrpy2sLapEBR1sms6moJxH424dTmE7T6yuX1FW6PMFvExXb9XpsQ2fMWHdu", 0)
      box.createdIn = Block("forB1", 1000)
      box.save()

      val customTx = txHandler.getPoolTransaction(minerAddress, value)
      customTx mustBe a [Transaction]
    }

    /**
     * Purpose: return same valid custom transaction.
     * Prerequisites: None.
     * Test Conditions:
     * * There is 1 box in DB and it wont change
     */
    "return same valid custom transaction" in {
      val txHandler = new TransactionHandler(client, lockAddress, withdrawAddress, testMnemonic)
      val value = 65000000000L
      val boxJs =
        """
          |{
          |      "boxId": "box1",
          |      "value": 67500000000,
          |      "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |      "assets": [],
          |      "creationHeight": 464846,
          |      "additionalRegisters": {},
          |      "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
          |      "index": 2
          |}
          |""".stripMargin
      val box = Box(Helper.convertToJson(boxJs), client.networkType, 0)
      box.createdIn = Block("forB1", 1000)
      box.save()

      val customTx1 = txHandler.getCustomTransaction(value)
      val customTx2 = txHandler.getCustomTransaction(value)

      customTx1 mustBe a [Transaction]
      customTx2 mustBe a [Transaction]

      customTx1 mustBe customTx2
    }

    /**
     * Purpose: return valid pool transaction.
     * Prerequisites: None.
     * Test Conditions:
     * * There is 1 box in DB
     */
    "return same valid pool transaction" in {
      val txHandler = new TransactionHandler(client, lockAddress, withdrawAddress, testMnemonic)
      NodeServlets.protectionAddress = ""
      val value = 65000000000L
      val boxJs =
        """
          |{
          |      "boxId": "5765a3352e8d2dcd6ad72870756ffb1de899178f76c893f14d5ce9f5f064ea6c",
          |      "value": 175000000000,
          |      "ergoTree": "100308cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc43508cd0373a3b15ecbe62e0bb94e05c6271de5bba07ef6435294157e585821cd6b0dd55208cd02dc931b18eaedcf92fb70dcb9216ede1f9d430f9ae1f7f436b81a186f2587cf51eb02ea02d193d0cddb6906db6503fed0730073017302",
          |      "assets": [],
          |      "creationHeight": 115596,
          |      "additionalRegisters": {},
          |      "transactionId": "7420eb8d7185903cc898b1eb694769d7153e17614fc43188dcdbc3de4404b7cb",
          |      "index": 0
          |}
          |""".stripMargin
      val box = Box(Helper.convertToJson(boxJs), "5Hg4a36kQGxB6zki3oCYbSfL3yfmhGCc4yYfRUwEdgqMWPbY57TAZozkvVunEsUmb5vgRU6iiFQzJkpaR8PJ7hfzbj8NNTf5Rmf2ou9LrmafJDD1ewX9YjqgGHWgJrpy2sLapEBR1sms6moJxH424dTmE7T6yuX1FW6PMFvExXb9XpsQ2fMWHdu", 0)
      box.createdIn = Block("forB1", 1000)
      box.save()

      val customTx1 = txHandler.getPoolTransaction(minerAddress, value)
      val customTx2 = txHandler.getPoolTransaction(minerAddress, value)

      customTx1 mustBe a [Transaction]
      customTx2 mustBe a [Transaction]

      customTx1 mustBe customTx2
    }

    /**
     * Purpose: return different valid custom transaction because input boxes have been spent.
     * Prerequisites: None.
     * Test Conditions:
     * * There is 1 box in DB and it wont change
     */
    "return different valid custom transaction" in {
      val txHandler = new TransactionHandler(client, lockAddress, withdrawAddress, testMnemonic)
      val value = 65000000000L
      var boxJs =
        """
          |{
          |      "boxId": "box1",
          |      "value": 67500000000,
          |      "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |      "assets": [],
          |      "creationHeight": 464846,
          |      "additionalRegisters": {},
          |      "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
          |      "index": 2
          |}
          |""".stripMargin
      var box = Box(Helper.convertToJson(boxJs), client.networkType, 0)
      box.createdIn = Block("forB1", 1000)
      box.save()

      val customTx1 = txHandler.getCustomTransaction(value)

      box.delete()
      boxJs =
        """
          |{
          |      "boxId": "box2",
          |      "value": 67500000000,
          |      "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |      "assets": [],
          |      "creationHeight": 464846,
          |      "additionalRegisters": {},
          |      "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
          |      "index": 2
          |}
          |""".stripMargin
      box = Box(Helper.convertToJson(boxJs), client.networkType, 0)
      box.createdIn = Block("forB1", 1000)
      box.save()
      val customTx2 = txHandler.getCustomTransaction(value)

      customTx1 mustBe a [Transaction]
      customTx2 mustBe a [Transaction]

      customTx1 mustNot be(customTx2)
    }

    /**
     * Purpose: return different valid custom transaction because input boxes have been spent.
     * Prerequisites: None.
     * Test Conditions:
     * * There is 1 box in DB
     */
    "return different valid pool transaction" in {
      val txHandler = new TransactionHandler(client, lockAddress, withdrawAddress, testMnemonic)
      NodeServlets.protectionAddress = ""
      val value = 65000000000L
      var boxJs =
        """
          |{
          |      "boxId": "5765a3352e8d2dcd6ad72870756ffb1de899178f76c893f14d5ce9f5f064ea6c",
          |      "value": 175000000000,
          |      "ergoTree": "100308cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc43508cd0373a3b15ecbe62e0bb94e05c6271de5bba07ef6435294157e585821cd6b0dd55208cd02dc931b18eaedcf92fb70dcb9216ede1f9d430f9ae1f7f436b81a186f2587cf51eb02ea02d193d0cddb6906db6503fed0730073017302",
          |      "assets": [],
          |      "creationHeight": 115596,
          |      "additionalRegisters": {},
          |      "transactionId": "7420eb8d7185903cc898b1eb694769d7153e17614fc43188dcdbc3de4404b7cb",
          |      "index": 0
          |}
          |""".stripMargin
      var box = Box(Helper.convertToJson(boxJs), "5Hg4a36kQGxB6zki3oCYbSfL3yfmhGCc4yYfRUwEdgqMWPbY57TAZozkvVunEsUmb5vgRU6iiFQzJkpaR8PJ7hfzbj8NNTf5Rmf2ou9LrmafJDD1ewX9YjqgGHWgJrpy2sLapEBR1sms6moJxH424dTmE7T6yuX1FW6PMFvExXb9XpsQ2fMWHdu", 0)
      box.createdIn = Block("forB1", 1000)
      box.save()
      val customTx1 = txHandler.getPoolTransaction(minerAddress, value)

      box.delete()
      boxJs =
        """
          |{
          |  "boxId": "f1d9cfa65e8fd913f336058e32075a94fdd32ee3f5d091dcacbfcd016030d8f1",
          |  "value": 70000000000,
          |  "ergoTree": "100308cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc43508cd0373a3b15ecbe62e0bb94e05c6271de5bba07ef6435294157e585821cd6b0dd55208cd02dc931b18eaedcf92fb70dcb9216ede1f9d430f9ae1f7f436b81a186f2587cf51eb02ea02d193d0cddb6906db6503fed0730073017302",
          |  "assets": [],
          |  "creationHeight": 115596,
          |  "additionalRegisters": {},
          |  "transactionId": "b766a831c1a7d7b41bc5fb2ad85bbc808e159dbdb845aa16833d5e8235646cbb",
          |  "index": 0
          |}
          |""".stripMargin
      box = Box(Helper.convertToJson(boxJs), "5Hg4a36kQGxB6zki3oCYbSfL3yfmhGCc4yYfRUwEdgqMWPbY57TAZozkvVunEsUmb5vgRU6iiFQzJkpaR8PJ7hfzbj8NNTf5Rmf2ou9LrmafJDD1ewX9YjqgGHWgJrpy2sLapEBR1sms6moJxH424dTmE7T6yuX1FW6PMFvExXb9XpsQ2fMWHdu", 0)
      box.createdIn = Block("forB1", 1000)
      box.save()
      val customTx2 = txHandler.getPoolTransaction(minerAddress, value)

      customTx1 mustBe a [Transaction]
      customTx2 mustBe a [Transaction]

      customTx1 mustNot be(customTx2)
    }
  }
}