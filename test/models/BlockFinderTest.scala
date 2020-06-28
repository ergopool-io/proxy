package models

import java.io.File

import helpers.Helper
import io.ebean.Ebean
import org.ergoplatform.appkit.NetworkType
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.db.DBApi
import play.api.db.evolutions.Evolutions
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

class BlockFinderTest extends PlaySpec with BeforeAndAfterAll with BeforeAndAfterEach {
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

  "BlockFinderTest" should {
    /**
     * Purpose: get block max height
     * Prerequisites: None.
     * Scenario: get max height when db is empty
     * Test Conditions:
     * * max height must be 0
     */
    "return valid max height 0" in {
      val blockFinder = new BlockFinder
      val ret = blockFinder.maxHeight()
      ret mustBe 0
    }

    /**
     * Purpose: get block max height
     * Prerequisites: None.
     * Scenario: get max height when db is not empty
     * Test Conditions:
     *  max height must be height of maximum block
     */
    "return valid max height of highest block" in {
      val blockFinder = new BlockFinder
      Block("1", 1023).save()
      Block("3", 100).save()
      Block("2", 10000).save()
      val ret = blockFinder.maxHeight()
      ret mustBe 10000
    }

    /**
     * Purpose: remove old unused blocks
     * Prerequisites:
     * * two old unused block
     * * two old used block
     * * one not old block
     * Scenario: remove old blocks which have no box
     * Test Conditions:
     *  only old unused blocks must be removed
     */
    "remove old unused blocks" in {
      val blockFinder = new BlockFinder
      val currentHeight = 10000
      val newBlock = Block("1", 9500)
      val u2 = Block("2", 8200)
      val u1 = Block("3", 8550)
      val oldUsed1 = Block("4", 70000)
      val oldUsed2 = Block("5", 50000)
      oldUsed1.save()
      oldUsed2.save()

      val box = Box(Helper.convertToJson(
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
          |""".stripMargin), NetworkType.TESTNET, 0)
      box.createdIn = oldUsed1
      box.spentIn = oldUsed2
      box.save()
      newBlock.save()
      u1.save()
      u2.save()

      Ebean.execute(() => {
        blockFinder.removeOldUnusedBlocks(currentHeight)
      })

      Vector("1", "4", "5").foreach(id => blockFinder.byBlockId(id) mustNot be(null))
      Vector("2", "3").foreach(id => blockFinder.byBlockId(id) mustBe null)
    }
  }
}
