package node

import java.io.File

import helpers.Helper
import models.{Block, BlockFinder, Box, BoxFinder}
import org.ergoplatform.appkit.NetworkType
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.db.DBApi
import play.api.db.evolutions.Evolutions
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import scalaj.http.HttpResponse

class BoxLoaderTest extends PlaySpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {

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

  /**
   * Purpose: load boxes into the DB.
   * Prerequisites: None.
   * Scenario: Mock boxLoader to return two boxes as old unspent boxes and check if methods will be call correctly.
   * Test Conditions:
   * * saveBoxes called 1 time
   * * loadLatestBoxes called 15 times
   */
  "BoxLoaderTest" should {
    "loadBoxes" in {
      val boxLoader = mock[BoxLoader]

      when(boxLoader.loadBoxes(2000, 4000)).thenCallRealMethod()
      val testBox1 = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "box1",
          |  "value": 93977000000,
          |  "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |  "assets": [],
          |  "creationHeight": 2600,
          |  "additionalRegisters": {},
          |  "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
          |  "index": 2
          |}
          |""".stripMargin), NetworkType.TESTNET, 0)

      val testBox2 = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "box2",
          |  "value": 9000000,
          |  "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |  "assets": [],
          |  "creationHeight": 2400,
          |  "additionalRegisters": {},
          |  "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
          |  "index": 2
          |}
          |""".stripMargin), NetworkType.TESTNET, 0)

      when(boxLoader.loadOldUnspentBoxes(2000, 4000 - 1440, 4000)).thenReturn(Vector[Box](testBox1, testBox2))
      when(boxLoader.loadLatestBoxes(anyInt(), anyInt(), anyInt())).thenReturn(Vector[Box]())
      when(boxLoader.loadChunkBoxes(any(), any(), any(), any())).thenCallRealMethod()
      when(boxLoader.chunkSize).thenReturn(500)
      boxLoader.loadBoxes(2000, 4000)

      verify(boxLoader, times(1)).saveBoxes(Vector[Box](testBox1, testBox2))
      verify(boxLoader, times(3)).loadLatestBoxes(any(), any(), any())
    }

    /**
     * Purpose: Load old unspent boxes.
     * Prerequisites: None.
     * Scenario: Mock fetch boxes to return two list of test boxes and check if loadOldUnspentBoxes will get them correctly.
     * Test Conditions:
     * * returned vector contain two boxes
     * * The boxes ids are ["box1", "box2"]
     */
    "loadOldUnspentBoxes" in {
      val client = mock[NodeClient]

      val boxFinder = new BoxFinder
      val blockFinder = new BlockFinder

      val boxLoader = mock[BoxLoader](withSettings().useConstructor(client, boxFinder, blockFinder))
      val testBox1 = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "box1",
          |  "value": 93977000000,
          |  "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |  "assets": [],
          |  "creationHeight": 464846,
          |  "additionalRegisters": {},
          |  "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
          |  "index": 2
          |}
          |""".stripMargin), NetworkType.TESTNET, 0)

      val testBox2 = Box(Helper.convertToJson(
        """
          |{
          |  "boxId": "box2",
          |  "value": 9000000,
          |  "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
          |  "assets": [],
          |  "creationHeight": 5678,
          |  "additionalRegisters": {},
          |  "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
          |  "index": 2
          |}
          |""".stripMargin), NetworkType.TESTNET, 0)

      when(boxLoader.fetchBoxes(ArgumentMatchers.eq(2001), ArgumentMatchers.eq(1500), any(), any())).thenReturn(Vector[Box](testBox1))
      when(boxLoader.fetchBoxes(ArgumentMatchers.eq(2501), ArgumentMatchers.eq(1440), any(), any())).thenReturn(Vector[Box](testBox2))
      when(boxLoader.loadOldUnspentBoxes(2000, 4000 - 1440, 4000)).thenCallRealMethod()
      when(boxLoader.loadChunkBoxes(any(), any(), any(), any())).thenCallRealMethod()
      when(boxLoader.chunkSize).thenReturn(500)
      val ret = boxLoader.loadOldUnspentBoxes(2000, 4000 - 1440, 4000)

      ret.size mustBe 2
      ret.map(_.id) must contain allOf("box1", "box2")
    }

    /**
     * Purpose: Fetch boxes.
     * Prerequisites: None.
     * Scenario: Mock function to return a test box as a box and mock box loader findBoxBlock method to return
     *           a test block as the test box creation and spending block and check if the returned box info is right.
     * Test Conditions:
     * * returned vector having size 1
     * * returned box createdIn is not null
     * * returned box createdIn block id is block1
     * * returned box spentIn is not null
     * * returned box spentIn block id is block1
     */
    "fetchBoxes" in {
      val client = mock[NodeClient]
      val boxFinder = new BoxFinder()
      val blockFinder = new BlockFinder()
      when(client.networkType).thenReturn(NetworkType.TESTNET)

      val mockFunc = mock[(Int, Int) => HttpResponse[Array[Byte]]]
      when(mockFunc.apply(0, 0))
        .thenReturn(
          HttpResponse[Array[Byte]](
            s"""
               |[
               |  {
               |    "box": {
               |      "boxId": "box1",
               |      "value": 93977000000,
               |      "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
               |      "assets": [],
               |      "creationHeight": 464846,
               |      "additionalRegisters": {},
               |      "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
               |      "index": 2
               |    },
               |    "inclusionHeight": 1000,
               |    "spent": true,
               |    "spendingTransaction": "sTx",
               |    "creationTransaction": "cTx"
               |  }
               |]
               |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]())
        )

      val boxLoader = mock[BoxLoader](withSettings().useConstructor(client, boxFinder, blockFinder))

      when(boxLoader.findBoxBlock("box1", "sTx")).thenReturn(Block("block1", 1000))
      when(boxLoader.findBoxBlock("box1", "cTx")).thenReturn(Block("block1", 1000))
      when(boxLoader.fetchBoxes(0, 0, 0, mockFunc)).thenCallRealMethod()

      val ret = boxLoader.fetchBoxes(0, 0, 0, mockFunc)

      ret.size mustBe 1
      ret.apply(0).createdIn mustNot be(null)
      ret.apply(0).createdIn.blockId mustBe "block1"
      ret.apply(0).spentIn mustNot be(null)
      ret.apply(0).spentIn.blockId mustBe "block1"
    }

    /**
     * Purpose: find the block of the box that the box was created in from node.
     * Prerequisites: None.
     * Scenario: Mock client to return a block with id block1 as a block in height 1000 and mock block finder to return
     *           null to get the block from node and test if findBoxBlock will return the right info for the passed box.
     * Test Conditions:
     * * returned block is not null
     * * returned block id is block1
     */
    "findBoxBlock" in {
      val client = mock[NodeClient]
      val boxFinder = new BoxFinder()
      val blockFinder = mock[BlockFinder]

      when(client.blocksAtHeight(1000))
        .thenReturn(
          HttpResponse[Array[Byte]](
            s"""
               |[
               |  "block2",
               |  "block1"
               |]
               |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]())
        )
      when(client.walletTransaction("tx1"))
        .thenReturn(
          HttpResponse[Array[Byte]](
            s"""
               |{
               |  "inclusionHeight": 1000
               |}
               |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]())
        )

      val boxLoader = mock[BoxLoader](withSettings().useConstructor(client, boxFinder, blockFinder))

      when(boxLoader.isTransactionInBlock("block2", "tx1")).thenReturn(false)
      when(boxLoader.isTransactionInBlock("block1", "tx1")).thenReturn(true)
      when(boxLoader.findBoxBlock("box1", "tx1")).thenCallRealMethod()
      when(blockFinder.byBlockId("block1")).thenReturn(null)

      val ret = boxLoader.findBoxBlock("box1", "tx1")

      ret mustNot be(null)
      ret.blockId mustBe "block1"
    }

    /**
     * Purpose: Check if the transaction is in one the blocks transactions.
     * Prerequisites: None.
     * Scenario: Mock client to return a test block body that has the transaction and check if isTransactionInBlock
     *           will return the right output.
     * Test Conditions:
     * * returned value is true
     */
    "isTransactionInBlock" in {
      val client = mock[NodeClient]
      val boxFinder = mock[BoxFinder]
      val blockFinder = mock[BlockFinder]

      when(client.blockTransactions("block1"))
        .thenReturn(
          HttpResponse[Array[Byte]](
            s"""
               |{
               |  "transactions": [
               |    {
               |      "id": "some tx"
               |    },
               |    {
               |      "id": "txId"
               |    }
               |  ]
               |}
               |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]())
        )

      when(blockFinder.byBlockId("block1")).thenReturn(Block("block1", 1000))

      val boxLoader = mock[BoxLoader](withSettings().useConstructor(client, boxFinder, blockFinder))

      when(boxLoader.isTransactionInBlock(any(), any())).thenCallRealMethod()

      val ret = boxLoader.isTransactionInBlock("block1", "txId")

      ret mustBe true
    }
  }
}