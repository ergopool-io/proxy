package pool

import helpers.Helper
import io.circe.Json
import node.{Proof, Share, Transaction}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.play.PlaySpec
import scalaj.http.HttpResponse

import scala.collection.mutable

class PoolQueueTest extends PlaySpec with MockitoSugar {

  "PoolQueueTest" should {

    /**
     * Purpose: Push shares into the pool queue
     * Prerequisites: None.
     * Scenario: Create a list of shares and push to pool and check the pool queue size.
     * Test Conditions:
     * * queue.size is 2
     * * callFutureRun had been called
     */
    "push shares" in {
      val pool = mock[Pool]

      val shares = Share(Helper.convertToJson(
        """
          |[
          |   {
          |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |      "w": "0204787c4a256cb31f0e1c3c0a65907cddf7fa64b50c29ec90d2f2e9311315d3c0",
          |      "n": "000013c2df7944ce",
          |      "d": 126332151464134121647421875202846731543931375566395403436590117338
          |   },
          |   {
          |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |      "w": "0204787c4a256cb31f0e1c3c0a65907cddf7fa64b50c29ec90d2f2e9311315d3c0",
          |      "n": "000013c2df7944ce",
          |      "d": 126332151464134121647421875202846731543931375566395403436590117338
          |   }
          |]
          |""".stripMargin))

      val queue: mutable.Queue[pool.PoolPacket] = mutable.Queue.empty
      when(pool.queue).thenReturn(queue)
      when(pool.push(shares: _*)).thenCallRealMethod()
      when(pool.isRunning).thenReturn(false)
      when(pool.runQueue()).thenCallRealMethod()
      pool.push(shares: _*)

      queue.size mustBe 2
      verify(pool).callFutureRun()
    }

    /**
     * Purpose: Push (Transaction, Proof) tuple into the pool queue
     * Prerequisites: None.
     * Scenario: Pass a mock transaction and a mock proof to pool and check the pool queue size.
     * Test Conditions:
     * * queue.size is 1
     */
    "push TransactionProof" in {
      val pool = mock[Pool]
      val mockedTx = mock[Transaction]
      val mockedProof = mock[Proof]

      val queue: mutable.Queue[pool.PoolPacket] = mutable.Queue.empty
      when(pool.queue).thenReturn(queue)
      when(pool.push(mockedTx, mockedProof)).thenCallRealMethod()
      pool.push(mockedTx, mockedProof)

      queue.size mustBe 1
    }

    /**
     * Purpose: Queue will remove shares when there is no transaction/proof
     * Prerequisites: None.
     * Scenario: Create a pool and pass two shares before a TxP. Pool should remove those two shares from queue without
     *           sending them to the pool server.
     * Test Conditions:
     * * queue.size is 0
     * * transaction is not null
     * * proof is not null
     */
    "run - clear shares in queue until next TxP if tx or proof are null" in {
      val pool = new Pool(_ => null) {
        maxChunkSize = 5.toShort
        override def sendPacket(onGoingQueue: mutable.Queue[Json], proof: Proof, transaction: Transaction): HttpResponse[Array[Byte]] = {
          HttpResponse[Array[Byte]](
            """
              |
              |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]()
          )
        }
        override def runQueue(): Unit = {}
      }

      val mockedTx = mock[Transaction]
      val mockedProof = mock[Proof]
      val shares = Share(Helper.convertToJson(
        """
          |[
          |   {
          |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |      "w": "0204787c4a256cb31f0e1c3c0a65907cddf7fa64b50c29ec90d2f2e9311315d3c0",
          |      "n": "000013c2df7944ce",
          |      "d": 126332151464134121647421875202846731543931375566395403436590117338
          |   },
          |   {
          |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |      "w": "0204787c4a256cb31f0e1c3c0a65907cddf7fa64b50c29ec90d2f2e9311315d3c0",
          |      "n": "000013c2df7944ce",
          |      "d": 126332151464134121647421875202846731543931375566395403436590117338
          |   }
          |]
          |""".stripMargin))
      pool.push(shares: _*)
      pool.push(mockedTx, mockedProof)

      pool.run(exitIfEmpty = true)
      pool.queue.size mustBe 0
      pool.transaction mustNot be(null)
      pool.proof mustNot be(null)
    }

    /**
     * Purpose: Queue will remove shares when there is no transaction/proof
     * Prerequisites: None.
     * Scenario: Create a pool and pass two shares after a TxP. Pool should set tx and proof and then send those two
     *           shares to the pool server.
     * Test Conditions:
     * * sendPacket is called
     * * queue.size is 0
     * * transaction is not null
     * * proof is not null
     */
    "run - sends packet and clear them if response is 200" in {
      var sendCalled = false
      val pool = new Pool(_ => null) {
        maxChunkSize = 5.toShort
        override def sendPacket(onGoingQueue: mutable.Queue[Json], proof: Proof, transaction: Transaction): HttpResponse[Array[Byte]] = {
          sendCalled = true
          HttpResponse[Array[Byte]](
            """
              |
              |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]()
          )
        }
        override def runQueue(): Unit = {}
      }

      val mockedTx = mock[Transaction]
      val mockedProof = mock[Proof]
      val shares = Share(Helper.convertToJson(
        """
          |[
          |   {
          |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |      "w": "0204787c4a256cb31f0e1c3c0a65907cddf7fa64b50c29ec90d2f2e9311315d3c0",
          |      "n": "000013c2df7944ce",
          |      "d": 126332151464134121647421875202846731543931375566395403436590117338
          |   },
          |   {
          |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
          |      "w": "0204787c4a256cb31f0e1c3c0a65907cddf7fa64b50c29ec90d2f2e9311315d3c0",
          |      "n": "000013c2df7944ce",
          |      "d": 126332151464134121647421875202846731543931375566395403436590117338
          |   }
          |]
          |""".stripMargin))
      pool.push(mockedTx, mockedProof)
      pool.push(shares: _*)

      pool.run(exitIfEmpty = true)
      sendCalled mustBe true
      pool.queue.size mustBe 0
      pool.transaction mustNot be(null)
      pool.proof mustNot be(null)
    }
  }
}
