package proxy

import java.io.File

import akka.util.ByteString
import models.{BlockFinder, BoxFinder}
import node._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.db.DBApi
import play.api.db.evolutions.Evolutions
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.mvc.RawBuffer
import play.api.test.FakeRequest
import play.test.Helpers.GET
import pool.Pool
import scalaj.http.HttpResponse

class ProxyTest extends PlaySpec with MockitoSugar with BeforeAndAfterAll {
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

  "ProxyTest" should {

    /**
     * Purpose: loadBoxesAndBlocks will return a function that gets newest boxes and blocks.
     * Prerequisites: None.
     * Scenario: pass mocked finders and loaders to proxy and verify that their functions has been called.
     * Test Conditions:
     * * blockLoader.loadLatestChainSlice has been called
     * * boxLoader.loadUnspentBoxes has been called
     * * boxFinder.removeOldSpentBoxes has been called
     * * blockFinder.removeOldUnusedBlocks has been called
     */
    "loadBoxesAndBlocks" in {
      val blockFinder = mock[BlockFinder]
      val boxFinder = mock[BoxFinder]
      val blockLoader = mock[BlockLoader]
      val boxLoader = mock[BoxLoader]
      when(blockFinder.maxHeight()).thenReturn(1000)
      val node = mock[NodeClient]
      when(node.info).thenReturn(
        HttpResponse[Array[Byte]](
          s"""
             |{
             |  "fullHeight": 2000
             |}
             |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]())
      )
      when(node.isWalletUnlocked).thenReturn(true)
      val proxy = new Proxy(node)
      proxy.loadBoxesAndBlocks(blockFinder, boxFinder, blockLoader, boxLoader)(deleteUnused = true)

      verify(blockLoader).handleForkedBlocks()
      verify(boxLoader).loadBoxes(1000, 2000)
      verify(boxFinder).removeOldSpentBoxes(2000)
      verify(blockFinder).removeOldUnusedBlocks(2000)
      verify(blockLoader).loadLatestChainSlice(1000, 2000)
    }

    /**
     * Purpose: Check headers of pool packets are in the right format.
     * Prerequisites: None.
     * Scenario: mock node and proxy to return test addresses and check the response
     * Test Conditions:
     * * response body must be right
     */
    "packetHeaders" in {
      val node = mock[NodeClient]
      val proxy = mock[Proxy](withSettings().useConstructor(node))

      when(node.minerAddress).thenReturn("minerAddress")
      when(proxy.lockAddress).thenReturn("lockAddress")
      when(proxy.withdrawAddress).thenReturn("withdrawAddress")
      when(node.pk).thenReturn("pk")

      when(proxy.packetHeaders).thenCallRealMethod()
      val ret = proxy.packetHeaders
      ret.toString().replaceAll("\\s", "") mustBe
        """
          |{
          |    "addresses": {
          |        "miner": "minerAddress",
          |        "lock": "lockAddress",
          |        "withdraw": "withdrawAddress"
          |    },
          |    "pk": "pk"
          |}
          |""".stripMargin.replaceAll("\\s", "")
    }

    /**
     * Purpose: the solution will be sent to the pool.
     * Prerequisites: None.
     * Scenario: mock pool and verify that the push method is called.
     * Test Conditions:
     * * pool.push is called
     */
    "sendSolution" in {
      val mockedPool = mock[Pool]
      val node = new NodeClient
      val proxy = new Proxy(node) {
        override lazy val pool: Pool = mockedPool
      }
      val bytes: ByteString = ByteString(
        """
          |{
          |   "pk": "test pk",
          |   "w": "test w",
          |   "n": "test n",
          |   "d": "test d",
          |}
          |""".stripMargin)
      val req = FakeRequest(GET, "/test/proxy").withHeaders(("Content-Type", "")).withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))

      proxy.sendSolution(req)
      verify(mockedPool).push(any())
    }

    /**
     * Purpose: check if getMiningCandidate will return the right response for the miner.
     * Prerequisites: None.
     * Scenario: mock pool and transaction handler to return test values and transactions and check if the method
     *           response is correct.
     * Test Conditions:
     * * response is correct for the miner
     */
    "getMiningCandidate" in {
      val mockedPool = mock[Pool]
      when(mockedPool.difficultyFactor).thenReturn(BigDecimal("10"))
      val node = mock[NodeClient]
      when(node.transactionFee).thenReturn(1)
      val txHandler = mock[TransactionHandler]
      val proxy = new Proxy(node) {
        override lazy val pool: Pool = mockedPool
        override lazy val transactionHandler: TransactionHandler = txHandler
      }

      when(node.miningCandidate).thenReturn(
        HttpResponse[Array[Byte]](
          s"""
             |{
             |  "msg": "the message",
             |  "b": 10,
             |  "pk": "the pk",
             |  "proof": {
             |    "msgPreimage": "msg pre image",
             |    "txProofs": [
             |      {
             |        "leaf": "poolTx",
             |        "levels": [
             |          "lvl1",
             |          "lvl2"
             |        ]
             |      },
             |      {
             |        "leaf": "customTx",
             |        "levels": [
             |          "lvl1",
             |          "lvl2"
             |        ]
             |      }
             |    ]
             |  }
             |}
             |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]())
      )

      val mockCustomTx = mock[Transaction]
      when(mockCustomTx.id).thenReturn("customTx")
      when(txHandler.getCustomTransaction(any())).thenReturn(mockCustomTx)
      val mockPoolTx = mock[Transaction]
      when(mockPoolTx.id).thenReturn("poolTx")
      when(txHandler.getPoolTransaction(any(), any())).thenReturn(mockPoolTx)

      val resp = proxy.getMiningCandidate
      helpers.Helper.ArrayByte(resp.body).toString.replaceAll("\\s", "") mustBe
        """
          |{
          |  "msg" : "the message",
          |  "b" : 10,
          |  "pk" : "the pk",
          |  "pb" : 100
          |}
          |""".stripMargin.replaceAll("\\s", "")
    }
  }
}
