package models

import java.io.File

import helpers.Helper
import org.ergoplatform.appkit.NetworkType
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.db.DBApi
import play.api.db.evolutions.Evolutions
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

class BoxFinderTest extends PlaySpec with BeforeAndAfterAll with BeforeAndAfterEach {
  lazy val appBuilder = new GuiceApplicationBuilder()
  lazy val injector: Injector = appBuilder.injector()
  lazy val databaseApi: DBApi = injector.instanceOf[DBApi]

  /**
   * Apply migrations to database before running units
   */
  override def beforeAll(): Unit = {
    Evolutions.applyEvolutions(databaseApi.database("default"))

    super.beforeAll()
  }

  /**
   * Remove database after all units
   */
  override def afterAll(): Unit = {
    Evolutions.cleanupEvolutions(databaseApi.database("default"))
    new File("test.db").delete()

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

  "BoxFinderTest" should {
    /**
     * Purpose: Get boxes with specified total value and address in include or not in exclude
     * Prerequisites: None.
     * Scenario: Create 4 boxes, one valid, a box with excluded address, box with forked createdIn, spent box with
     *           forked spentIn. Check if the valid box and spent box with forked spentIn are in the response
     * Test Conditions:
     * * There is 2 blocks in DB
     * * The blocks ids are ["box1", "box2"]
     */
    "unspentBoxesWithTotalValue" in {
      val excludedAddressBox = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "invalid box1",
          |  "value": 100,
          |  "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |  "assets": [],
          |  "creationHeight": 1001,
          |  "additionalRegisters": {},
          |  "transactionId": "txId",
          |  "index": 1
          |}
          |""".stripMargin), NetworkType.TESTNET, 1000)
      excludedAddressBox.createdIn = Block("exBlock", 1000)

      val validBox1 = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "box1",
          |  "value": 190,
          |  "ergoTree": "0008cd028333f9f7454f8d5ff73dbac9833767ed6fc3a86cf0a73df946b32ea9927d9197",
          |  "assets": [],
          |  "creationHeight": 1002,
          |  "additionalRegisters": {},
          |  "transactionId": "txId",
          |  "index": 1
          |}
          |""".stripMargin), NetworkType.TESTNET, 1000)
      validBox1.createdIn = Block("valBlock1", 1000)

      val spentBox = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "spentBlock",
          |  "value": 20,
          |  "ergoTree": "0008cd028333f9f7454f8d5ff73dbac9833767ed6fc3a86cf0a73df946b32ea9927d9197",
          |  "assets": [],
          |  "creationHeight": 1003,
          |  "additionalRegisters": {},
          |  "transactionId": "txId",
          |  "index": 1
          |}
          |""".stripMargin), NetworkType.TESTNET, 1000)
      spentBox.createdIn = Block("block1", 1000)
      spentBox.spentIn = Block("spendingBlock", 1000)

      val overHeightBox = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "overHeightBox",
          |  "value": 20,
          |  "ergoTree": "0008cd028333f9f7454f8d5ff73dbac9833767ed6fc3a86cf0a73df946b32ea9927d9197",
          |  "assets": [],
          |  "creationHeight": 1005,
          |  "additionalRegisters": {},
          |  "transactionId": "txId",
          |  "index": 1
          |}
          |""".stripMargin), NetworkType.TESTNET, 2000)
      overHeightBox.createdIn = Block("overHeightBlock", 2000)

      val validBox2 = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "box2",
          |  "value": 20,
          |  "ergoTree": "0008cd028333f9f7454f8d5ff73dbac9833767ed6fc3a86cf0a73df946b32ea9927d9197",
          |  "assets": [],
          |  "creationHeight": 1004,
          |  "additionalRegisters": {},
          |  "transactionId": "txId",
          |  "index": 1
          |}
          |""".stripMargin), NetworkType.TESTNET, 1500)
      validBox2.createdIn = Block("block2", 1500)

      excludedAddressBox.save()
      validBox1.save()
      spentBox.save()
      overHeightBox.save()
      validBox2.save()

      val boxFinder = new BoxFinder
      val ret = boxFinder.unspentBoxesWithTotalValue(200L,
        include = Vector[String]("3WwbzW6u8hKWBcL1W7kNVMr25s2UHfSBnYtwSHvrRQt7DdPuoXrt"), maxHeight = 1500)

      ret.size mustBe 2
      ret.iterator.map(_.id).toVector must contain allOf("box1", "box2")
    }

    /**
     * Purpose: remove old spent boxes
     * Prerequisites: None.
     * Scenario: Create 5 boxes, two unspent, one spent but not old, two spent and old, must remove 2 old boxes
     * Test Conditions:
     * * There is 4 blocks in DB
     */
    "remove old boxes but not any other" in {
      val currentHeight = 5000
      val unspentBox1 = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "unspent1",
          |  "value": 20,
          |  "ergoTree": "0008cd028333f9f7454f8d5ff73dbac9833767ed6fc3a86cf0a73df946b32ea9927d9197",
          |  "assets": [],
          |  "creationHeight": 1003,
          |  "additionalRegisters": {},
          |  "transactionId": "txId",
          |  "index": 1
          |}
          |""".stripMargin), NetworkType.TESTNET, 0)
      unspentBox1.createdIn = {
        val block = Block("block1", 1000)
        block
      }
      unspentBox1.save()

      val unspentBox2 = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "unspent2",
          |  "value": 20,
          |  "ergoTree": "0008cd028333f9f7454f8d5ff73dbac9833767ed6fc3a86cf0a73df946b32ea9927d9197",
          |  "assets": [],
          |  "creationHeight": 1003,
          |  "additionalRegisters": {},
          |  "transactionId": "txId",
          |  "index": 1
          |}
          |""".stripMargin), NetworkType.TESTNET, 0)
      unspentBox2.createdIn = {
        val block = Block("block1", 1000)
        block
      }
      unspentBox2.save()

      val newSpent = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "newSpent",
          |  "value": 20,
          |  "ergoTree": "0008cd028333f9f7454f8d5ff73dbac9833767ed6fc3a86cf0a73df946b32ea9927d9197",
          |  "assets": [],
          |  "creationHeight": 1003,
          |  "additionalRegisters": {},
          |  "transactionId": "txId",
          |  "index": 1
          |}
          |""".stripMargin), NetworkType.TESTNET, 0)
      newSpent.createdIn = {
        val block = Block("block1", 1000)
        block
      }
      newSpent.spentIn = {
        val block = Block("block2", currentHeight - 200)
        block
      }
      newSpent.save()

      val oldSpent1 = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "oldSpent1",
          |  "value": 20,
          |  "ergoTree": "0008cd028333f9f7454f8d5ff73dbac9833767ed6fc3a86cf0a73df946b32ea9927d9197",
          |  "assets": [],
          |  "creationHeight": 1003,
          |  "additionalRegisters": {},
          |  "transactionId": "txId",
          |  "index": 1
          |}
          |""".stripMargin), NetworkType.TESTNET, 0)
      oldSpent1.createdIn = {
        val block = Block("block1", 1000)
        block
      }
      oldSpent1.spentIn = {
        val block = Block("block3", currentHeight - 1500)
        block
      }
      oldSpent1.save()

      val oldSpent2 = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "oldSpent2",
          |  "value": 20,
          |  "ergoTree": "0008cd028333f9f7454f8d5ff73dbac9833767ed6fc3a86cf0a73df946b32ea9927d9197",
          |  "assets": [],
          |  "creationHeight": 1003,
          |  "additionalRegisters": {},
          |  "transactionId": "txId",
          |  "index": 1
          |}
          |""".stripMargin), NetworkType.TESTNET, 0)
      oldSpent2.createdIn = {
        val block = Block("block1", 1000)
        block
      }
      oldSpent2.spentIn = {
        val block = Block("block4", currentHeight - 1600)
        block
      }
      oldSpent2.save()
      val boxFinder = new BoxFinder
      boxFinder.removeOldSpentBoxes(currentHeight)

      var boxes: Vector[Box] = Vector()
      Vector("unspent1", "unspent2", "newSpent", "oldSpent1", "oldSpent2").foreach(f => {
        if (boxFinder.boxExists(f))
          boxes = boxes :+ boxFinder.byBoxId(f)
      })
      boxes.size mustBe 3
      boxes.iterator.map(_.id).toVector must contain allOf("unspent1", "unspent2", "newSpent")
    }
    /**
     * Purpose: get box count
     * Prerequisites: None.
     * Scenario: Create 5 boxes, two unspent
     * Test Conditions:
     * * box count must return 2 when all passed
     * * box count must return 1 when only a portion of box ids pass to the method
     */
    "return unspent boxes count" in {
      var boxes: Vector[Box] = Vector()
      (1 to 5).foreach(id => {
        val b1 = Box(Helper.convertToJson(
          s"""
            |{
            |  "boxId": "$id",
            |  "value": 20,
            |  "ergoTree": "0008cd028333f9f7454f8d5ff73dbac9833767ed6fc3a86cf0a73df946b32ea9927d9197",
            |  "assets": [],
            |  "creationHeight": 1003,
            |  "additionalRegisters": {},
            |  "transactionId": "txId",
            |  "index": 1
            |}
            |""".stripMargin), NetworkType.TESTNET, 0)
        b1.createdIn = {
          val block = Block("block1", 1000)
          block
        }
        boxes = boxes :+ b1
      })
      (0 to 1).foreach(id => {
        boxes(id).spentIn = Block("spentBlock", 1002)
        boxes(id).save()
      })
      (2 to 4).foreach(id => boxes(id).save())

      val boxFinder = new BoxFinder
      boxFinder.getAvailableBoxesCount(Vector("1", "2", "3", "4", "5")) mustBe 3
      boxFinder.getAvailableBoxesCount(Vector("2", "3", "4", "5")) mustBe 3
      boxFinder.getAvailableBoxesCount(Vector("2", "5")) mustBe 1

    }

  }
}
