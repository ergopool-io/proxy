package node

import java.io.File

import helpers.Helper
import akka.util.ByteString
import models.{Block, BlockFinder, Box, BoxFinder}
import org.mockito.ArgumentMatchers.any
import org.ergoplatform.appkit.NetworkType
import org.mockito.Mockito.{when, withSettings}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.db.DBApi
import play.api.db.evolutions._
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import scalaj.http.HttpResponse

import scala.collection.JavaConverters._

class BlockLoaderTest extends PlaySpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach {
  type Response = HttpResponse[Array[Byte]]
  lazy val appBuilder = new GuiceApplicationBuilder()
  lazy val injector: Injector = appBuilder.injector()
  lazy val databaseApi: DBApi = injector.instanceOf[DBApi]

  var box0: Box = _
  var box1: Box = _
  var box3: Box = _

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

  override def beforeEach(): Unit = {
    box0 = Box(Helper.convertToJson(
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

    box1 = Box(Helper.convertToJson(
      """
        |{
        |  "boxId": "box2",
        |  "value": 93977000000,
        |  "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
        |  "assets": [],
        |  "creationHeight": 464846,
        |  "additionalRegisters": {},
        |  "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
        |  "index": 2
        |}
        |""".stripMargin), NetworkType.TESTNET, 0)

    box3 = Box(Helper.convertToJson(
      """
        |{
        |  "boxId": "box3",
        |  "value": 93977000000,
        |  "ergoTree": "0008cd0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
        |  "assets": [],
        |  "creationHeight": 464846,
        |  "additionalRegisters": {},
        |  "transactionId": "017ed654b5c37851debb807534f21e458f8f77a6b30d510326ee297b0148f55a",
        |  "index": 2
        |}
        |""".stripMargin), NetworkType.TESTNET, 0)


    super.beforeEach()
  }

  /**
   * Remove all data after each unit
   */
  override protected def afterEach(): Unit = {
    new BoxFinder().query().delete()
    new BlockFinder().query().delete()
    super.afterEach()
  }


  "BlockLoaderTest" should {

    /**
     * Purpose: remove forked blocks
     * Prerequisites: None.
     * Scenario: create some forked blocks with boxes
     * Test Conditions:
     * * forked blocks must be removed
     * * boxes with spent_in in those blocks must have null spent_in
     * * boxes with created_in in those blocks must be removed
     */
    "handle forked blocks" in {
      val client = mock[NodeClient]
      val boxFinder = new BoxFinder
      val blockFinder = new BlockFinder
      val blockLoader = mock[BlockLoader](withSettings().useConstructor(client, boxFinder, blockFinder))

      val b0 = Block("block0", 1000)
      b0.save()
      val b1 = Block("block1", 1001)
      b1.save()
      val b2 = Block("forkedBlock1", 1002)
      b2.save()
      val b3 = Block("forkedBlock2", 1003)
      b3.save()

      when(blockLoader.mainChainAndDBIntersectionPoint()).thenReturn(b1)
      when(blockLoader.handleForkedBlocks()).thenCallRealMethod()

      box0.createdIn = b0
      box0.save()
      box1.createdIn = b1
      box1.spentIn = b2
      box1.save()
      box3.createdIn = b3
      box3.save()

      blockLoader.handleForkedBlocks()

      val blocks = blockFinder.query().findList()
      blocks.size() mustBe 2
      blocks.asScala.iterator.map(_.blockId).toVector must contain allOf("block0", "block1")

      val boxes = boxFinder.query().findList()
      boxes.size() mustBe 2
      boxes.asScala.iterator.map(_.id).toVector must contain allOf("box1", "box2")
      boxFinder.byId("box2").spentIn mustBe null
    }

    /**
     * Purpose: load blocks with empty db
     * Prerequisites: db is empty.
     * Scenario: load blocks when db is empty
     * Test Conditions:
     * * save last 1440 blocks
     */
    "load latest blocks when db is empty" in {
      val client = mock[NodeClient]
      val boxFinder = new BoxFinder
      val blockFinder = new BlockFinder
      val blockLoader = mock[BlockLoader](withSettings().useConstructor(client, boxFinder, blockFinder))
      val currentHeight = 100000
      val blocks: Vector[Block] = (currentHeight - 1440 to currentHeight).map(id => Block(id.toString, id)).toVector
      when(blockLoader.fetchBlocks(currentHeight - 1440, currentHeight)).thenReturn(blocks)
      when(blockLoader.loadLatestChainSlice(0, currentHeight)).thenCallRealMethod()

      blockLoader.loadLatestChainSlice(0, currentHeight)
      blockFinder.query().findCount() mustBe 1441
    }

    /**
     * Purpose: load blocks
     * Prerequisites: db is not empty
     * Scenario: load blocks chunk by chunk
     * Test Conditions:
     * * load blocks from 98 to 203 and save them
     * * spent boxes of these blocks must be saved
     */
    "load blocks" in {
      val client = mock[NodeClient]
      val boxFinder = new BoxFinder
      val blockFinder = new BlockFinder
      val blockLoader = mock[BlockLoader](withSettings().useConstructor(client, boxFinder, blockFinder))

      val b0 = Block("block0", 97)
      b0.save()
      val b1 = Block("block1", 98)
      b1.save()
      val b2 = Block("block3", 99)
      b2.save()
      box0.createdIn = b0
      box0.save()
      box1.createdIn = b1
      box1.save()
      box3.createdIn = b2
      box3.save()

      val dbHeight = 99
      val currentHeight = 203
      when(blockLoader.loadLatestChainSlice(dbHeight, currentHeight)).thenCallRealMethod()
      val bl1: Vector[Block] = (100 to 199).map(id => Block(id.toString, id)).toVector
      when(blockLoader.fetchBlocks(100, 199)).thenReturn(bl1)
      val bl2 = (200 to 203).map(id => Block(id.toString, id)).toVector
      when(blockLoader.fetchBlocks(200, 203)).thenReturn(bl2)
      box0.spentIn = bl1(5)
      box3.spentIn = bl2(1)
      when(blockLoader.spentBoxes(bl1)).thenReturn(Vector(box0))
      when(blockLoader.spentBoxes(bl2)).thenReturn(Vector(box3))
      when(blockLoader.chunkSize).thenReturn(100)

      blockLoader.loadLatestChainSlice(dbHeight, currentHeight)
      val blocks = blockFinder.query().findList()
      blocks.size() mustBe 107
      val boxes = boxFinder.query().findList()
      boxes.size() mustBe 3
      boxFinder.byId("box1").spentIn mustBe bl1(5)
      boxFinder.byId("box2").spentIn mustBe null
      boxFinder.byId("box3").spentIn mustBe bl2(1)
    }

    /**
     * Purpose: Find which blocks in DB had been forked.
     * Prerequisites: None.
     * Scenario: Mock client to return two blocks and save two test blocks that one of them is in client response. The
     * returned block from the method should be the intersection block.
     * Test Conditions:
     * * returned block id is block1
     */
    "mainChainAndDBIntersectionPoint" in {
      val client = mock[NodeClient]
      val blockFinder = new BlockFinder
      val boxFinder = new BoxFinder()

      when(client.blocksRange(901, 1001))
        .thenReturn(
          HttpResponse[Array[Byte]](
            s"""
               |[
               |  "block1",
               |  "block3"
               |]
               |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]())
        )

      Block("block1", 1000).save()
      Block("block2", 1001).save()

      val blockLoader = mock[BlockLoader](withSettings().useConstructor(client, boxFinder, blockFinder))
      when(blockLoader.mainChainAndDBIntersectionPoint()).thenCallRealMethod()
      when(blockLoader.blocksChunk(1001)).thenCallRealMethod()
      when(blockLoader.chunkSize).thenReturn(100)
      val bytes = ByteString(
        s"""
          |["block0", "block1", "block3"]
          |""".stripMargin)
      val res: Response = HttpResponse[Array[Byte]](bytes.toArray, 200, Map("Content-Type" -> Vector("application/json")))
      when(client.blocksRange(1001 - 100 + 1, 1001)).thenReturn(res)

      val ret = blockLoader.mainChainAndDBIntersectionPoint()
      ret.blockId mustBe "block1"
    }

    /**
     * Purpose: Find which blocks in DB had been forked.
     * Prerequisites: None.
     * Scenario: Mock client to return two blocks and save two test blocks that one of them is in client response. The
     * returned block from the method should be the intersection block.
     * Test Conditions:
     * * returned block id is block1
     */
    "return blocks in fetchBlock method" in {
      val client = mock[NodeClient]
      val blockFinder = new BlockFinder
      val boxFinder = new BoxFinder()

      when(client.blocksRange(901, 1001))
        .thenReturn(
          HttpResponse[Array[Byte]](
            s"""
               |[
               |  "block1",
               |  "block3"
               |]
               |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]())
        )


      val blockLoader = mock[BlockLoader](withSettings().useConstructor(client, boxFinder, blockFinder))
      when(blockLoader.fetchBlocks(901, 1001)).thenCallRealMethod()

      val ret = blockLoader.fetchBlocks(901, 1001)
      ret.size mustBe 2
      ret.iterator.map(_.blockId).toVector must contain allOf("block1", "block3")
    }

    /**
     * Purpose: get spent boxes of blocks
     * Prerequisites: None.
     * Scenario: get spent boxes of blocks regarding boxes in the DB
     * Test Conditions:
     * * spent boxes which are present in DB must be returned
     */
    "return spent boxes of blocks" in {
      val client = mock[NodeClient]
      val blockFinder = new BlockFinder
      val boxFinder = new BoxFinder()

      when(client.blockTransactions("block0"))
        .thenReturn(
          HttpResponse[Array[Byte]](
            s"""
               |{
               |  "transactions": [
               |    {
               |      "inputs": [
               |        {
               |          "boxId": "box1"
               |        },
               |        {
               |          "boxId": "some other box"
               |        }
               |      ]
               |    }
               |  ]
               |}
               |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]())
        )

      when(client.blockTransactions("block1"))
        .thenReturn(
          HttpResponse[Array[Byte]](
            s"""
               |{
               |  "transactions": [
               |    {
               |      "inputs": [
               |        {
               |          "boxId": "box4"
               |        },
               |        {
               |          "boxId": "some other box 2"
               |        }
               |      ]
               |    },
               |    {
               |      "inputs": [
               |        {
               |          "boxId": "box3"
               |        },
               |        {
               |          "boxId": "some other box 3"
               |        }
               |      ]
               |    }
               |  ]
               |}
               |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]())
        )

      box0.createdIn = Block("0", 1)
      box0.save()
      box1.createdIn = Block("0", 1)
      box1.save()
      box3.createdIn = Block("0", 1)
      box3.save()

      val blockLoader = mock[BlockLoader](withSettings().useConstructor(client, boxFinder, blockFinder))
      when(blockLoader.spentBoxes(any())).thenCallRealMethod()

      val ret = blockLoader.spentBoxes(Vector(Block("block0", 100), Block("block1", 200)))
      ret.size mustBe 2
      ret.iterator.map(_.id).toVector must contain allOf("box1", "box3")
      ret(0).spentIn.blockId mustBe "block0"
      ret(1).spentIn.blockId mustBe "block1"
    }
  }
}
