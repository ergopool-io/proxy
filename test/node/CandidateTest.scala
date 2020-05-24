package node

import helpers.Helper
import io.circe.Json
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import scalaj.http.HttpResponse

class CandidateTest extends PlaySpec with MockitoSugar {
  val mockSendTxP: (Transaction, Proof) => Unit = mock[(Transaction, Proof) => Unit]

  "CandidateTest - candidate" should {
    val body = Helper.convertToJson(
      """
        |{
        |  "msg": "first msg",
        |  "b": 1000,
        |  "pk": "pk",
        |  "proof": null
        |}
        |""".stripMargin)
    val client: NodeClient = mock[NodeClient]

    /**
     * Purpose: Send proof to pool if transactions exist in proof and msg is changed.
     * Prerequisites: Noting.
     * Scenario: Mock Candidate and its parameters and return true on isMsgChanged and isTxsExistInProof.
     * Test Conditions:
     * * verify sendProof of poolClient is called.
     * * response is a filtered /mining/candidate response for miner
     */
    "txs in proof - msg changed" in {
      val mockedTx = mock[Transaction]
      when(mockedTx.id).thenReturn("test")
      val candidate = mock[Candidate](withSettings().useConstructor(body, client, null, mockedTx, BigDecimal("0"), mockSendTxP))
      when(candidate.proof).thenReturn(Json.Null)
      when(candidate.candidate).thenCallRealMethod()
      when(candidate.isTxsExistInProof).thenReturn(true)
      when(candidate.isMsgChanged).thenReturn(true)
      val ret = candidate.candidate
      ret.toString().replaceAll("\\s", "") mustBe
        """
          |{
          |  "msg": "first msg",
          |  "b": 1000,
          |  "pk": "pk",
          |  "pb": 0
          |}
          |""".stripMargin.replaceAll("\\s", "")
    }

    /**
     * Purpose: Don't send proof to pool if transactions exist in proof but msg is not changed.
     * Prerequisites: Noting.
     * Scenario: Mock Candidate and its parameters and return true on isTxsExistInProof and false on isMsgChanged.
     * Test Conditions:
     * * verify sendProof of poolClient is not called.
     * * response is a filtered /mining/candidate response for miner
     */
    "txs in proof - msg not changed" in {
      val candidate = mock[Candidate](withSettings().useConstructor(body, client, null, null, BigDecimal("0"), mockSendTxP))
      when(candidate.candidate).thenCallRealMethod()
      when(candidate.isTxsExistInProof).thenReturn(true)
      when(candidate.isMsgChanged).thenReturn(false)
      val ret = candidate.candidate
      ret.toString().replaceAll("\\s", "") mustBe
        """
          |{
          |  "msg": "first msg",
          |  "b": 1000,
          |  "pk": "pk",
          |  "pb": 0
          |}
          |""".stripMargin.replaceAll("\\s", "")
    }

    /**
     * Purpose: Send transactions to /mining/candidateWithTxs if transactions doesn't exist in proof.
     * Prerequisites: Noting.
     * Scenario: Mock Candidate and its parameters and return false on isTxsExistInProof and mock client to return
     *           success response on candidateWithTxs.
     * Test Conditions:
     * * verify send of poolClient is called.
     * * response is a filtered /mining/candidate response for miner
     */
    "txs not in proof" in {
      val mockedTx = mock[Transaction]
      when(mockedTx.id).thenReturn("test")
      val candidate = mock[Candidate](withSettings().useConstructor(body, client, null, mockedTx, BigDecimal("0"), mockSendTxP))
      when(candidate.candidate).thenCallRealMethod()
      when(candidate.isTxsExistInProof).thenReturn(false)
      when(client.candidateWithTxs(Vector[Transaction](null, mockedTx)))
        .thenReturn(
          HttpResponse[Array[Byte]](
            body.toString().map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]()
          )
        )
      val ret = candidate.candidate
      ret.toString().replaceAll("\\s", "") mustBe
        """
          |{
          |  "msg": "first msg",
          |  "b": 1000,
          |  "pk": "pk",
          |  "pb": 0
          |}
          |""".stripMargin.replaceAll("\\s", "")
    }
  }

  "CandidateTest isTransactionInUnconfirmedTransactions" should {
    val body = Helper.convertToJson(
      """
        |{
        |  "msg": "first msg",
        |  "b": 1000,
        |  "pk": "pk",
        |  "proof": null
        |}
        |""".stripMargin)
    val client: NodeClient = mock[NodeClient]
    val candidate = mock[Candidate](withSettings().useConstructor(body, client, null, null, BigDecimal("0"), mockSendTxP))
    val testTx = mock[Transaction]
    when(testTx.id).thenReturn("txId")
    when(candidate.isTransactionInUnconfirmedTransactions(testTx)).thenCallRealMethod()

    /**
     * Purpose: Check if transaction is in unconfirmed transactions.
     * Prerequisites: Noting.
     * Scenario: Mock client to return a list of transactions that the test transaction is among them and pass the test
     *           transaction to the method.
     * Test Conditions:
     * * returned value is true.
     */
    "exists" in {
      when(client.unconfirmedTransactions(0, 50)).thenReturn(
        HttpResponse[Array[Byte]](
          """
            |[
            |   {
            |     "id": "txId"
            |   },
            |   {
            |     "id": "anotherId"
            |   }
            |]
            |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]()
        )
      )

      val ret = candidate.isTransactionInUnconfirmedTransactions(testTx)
      ret mustBe true
    }

    /**
     * Purpose: Check if transaction is not in unconfirmed transactions.
     * Prerequisites: Noting.
     * Scenario: Mock client to return a list of transactions that the test transaction is not among them and pass the
     *           test transaction to the method.
     * Test Conditions:
     * * returned value is false.
     */
    "not exists" in {
      when(client.unconfirmedTransactions(0, 50)).thenReturn(
        HttpResponse[Array[Byte]](
          """
            |[
            |   {
            |     "id": "some other id"
            |   }
            |]
            |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]()
        )
      )
      when(client.unconfirmedTransactions(50, 50)).thenReturn(
        HttpResponse[Array[Byte]](
          "[]".map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]()
        )
      )

      val ret = candidate.isTransactionInUnconfirmedTransactions(testTx)
      ret mustBe false
    }
  }

  "CandidateTest isTxsExistInProof" should {
    val protectedTx = mock[Transaction]
    when(protectedTx.id).thenReturn("prtx")

    val poolTx = mock[Transaction]
    when(poolTx.id).thenReturn("potx")

    val client: NodeClient = mock[NodeClient]
    val transactionHandler: TransactionHandler = mock[TransactionHandler]
    when(transactionHandler.getCustomTransaction(67500000000L)).thenReturn(protectedTx)
    when(transactionHandler.getPoolTransaction("poolAddr", 67500000000L)).thenReturn(poolTx)

    /**
     * Purpose: Check if both pool and protected transactions are in proof.
     * Prerequisites: Noting.
     * Scenario: pass a candidate body to class that has both transactions in its proof.
     * Test Conditions:
     * * returned value is true.
     */
    "both exist" in {
      val body = Helper.convertToJson(
        """
          |{
          |  "msg": "first msg",
          |  "b": 1000,
          |  "pk": "pk",
          |  "proof": {
          |     "txProofs": [
          |       {"leaf": "potx"},
          |       {"leaf": "prtx"}
          |     ]
          |  }
          |}
          |""".stripMargin)
      val candidate = new Candidate(body, client, protectedTx, poolTx, BigDecimal("0"), mockSendTxP)

      val ret = candidate.isTxsExistInProof
      ret mustBe true
    }

    /**
     * Purpose: Check if both pool and protected transactions are in proof.
     * Prerequisites: Noting.
     * Scenario: pass a candidate body to class that has one of the transactions in its proof.
     * Test Conditions:
     * * returned value is false.
     */
    "one exists" in {
      val body = Helper.convertToJson(
        """
          |{
          |  "msg": "first msg",
          |  "b": 1000,
          |  "pk": "pk",
          |  "proof": {
          |     "txProofs": [
          |       {"leaf": "another id"},
          |       {"leaf": "prtx"}
          |     ]
          |  }
          |}
          |""".stripMargin)
      val candidate = new Candidate(body, client, protectedTx, poolTx, BigDecimal("0"), mockSendTxP)

      val ret = candidate.isTxsExistInProof
      ret mustBe false
    }

    /**
     * Purpose: Check if both pool and protected transactions are in proof.
     * Prerequisites: Noting.
     * Scenario: pass a candidate body to class that has none of the transactions in its proof.
     * Test Conditions:
     * * returned value is false.
     */
    "none of them exist" in {
      val body = Helper.convertToJson(
        """
          |{
          |  "msg": "first msg",
          |  "b": 1000,
          |  "pk": "pk",
          |  "proof": {
          |     "txProofs": [
          |       {"leaf": "another id"},
          |       {"leaf": "another id 2"}
          |     ]
          |  }
          |}
          |""".stripMargin)
      val candidate = new Candidate(body, client, protectedTx, poolTx, BigDecimal("0"), mockSendTxP)

      val ret = candidate.isTxsExistInProof
      ret mustBe false
    }
  }

  "CandidateTest" should {

    /**
     * Purpose: Check if pool transaction is null, send protected transaction to network if it's not already sent.
     * Prerequisites: Noting.
     * Scenario: Mock class to have a null pool transaction and isTransactionInUnconfirmedTransactions to return false.
     * Test Conditions:
     * * throws a PoolTxIsNull exception
     * * sendErgoTransaction is called
     */
    "getResponse" in {
      val body = Helper.convertToJson(
        """
          |{
          |  "msg": "first msg",
          |  "b": 1000,
          |  "pk": "pk",
          |  "proof": null
          |}
          |""".stripMargin)
      val client: NodeClient = mock[NodeClient]
      when(client.sendErgoTransaction(ArgumentMatchers.any())).thenReturn(
        HttpResponse[Array[Byte]](
          Array[Byte](), 200, Map[String, IndexedSeq[String]]()
        )
      )
      val mockTx = mock[Transaction]
      val candidate = mock[Candidate](withSettings().useConstructor(body, client, mockTx, null, BigDecimal("0"), mockSendTxP))

      when(candidate.getResponse).thenCallRealMethod()
      when(candidate.isTransactionInUnconfirmedTransactions(null)).thenReturn(false)
      assertThrows[PoolTxIsNull] {
        candidate.getResponse
      }
      verify(client).sendErgoTransaction(mockTx)
    }

    /**
     * Purpose: Check if msg is changed in candidate body.
     * Prerequisites: Noting.
     * Scenario: set blockHeader to sth other than what is in the candidate body
     * Test Conditions:
     * * returned value is true
     */
    "isMsgChanged" in {
      val body = Helper.convertToJson(
        """
          |{
          |  "msg": "first msg",
          |  "b": 1000,
          |  "pk": "pk",
          |  "proof": null
          |}
          |""".stripMargin)
      val client: NodeClient = mock[NodeClient]
      val candidate = mock[Candidate](withSettings().useConstructor(body, client, null, null, BigDecimal("0"), mockSendTxP))
      client.blockHeader = ""

      when(candidate.isMsgChanged).thenCallRealMethod()
      val ret = candidate.isMsgChanged
      ret mustBe true
    }
  }
}
