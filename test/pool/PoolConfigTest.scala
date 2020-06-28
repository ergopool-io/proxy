package pool

import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.play.PlaySpec
import scalaj.http.HttpResponse

class PoolConfigTest extends PlaySpec with MockitoSugar {

  "PoolConfigTest" should {

    /**
     * Purpose: Show pool config in a string.
     * Prerequisites: None.
     * Scenario: Mock pool to return test value and check toString response.
     * Test Conditions:
     * * values are in a right format
     */
    "toString" in {
      val pool = mock[Pool]
      when(pool.connection).thenReturn("connection")
      when(pool.walletAddress).thenReturn("walletAddress")
      when(pool.difficultyFactor).thenReturn(BigDecimal("10"))
      when(pool.transactionRequestsValue).thenReturn(10)
      when(pool.maxChunkSize).thenReturn(10.toShort)
      when(pool.toString).thenCallRealMethod()

      pool.toString mustBe
        """
          |{
          |   "connection": "connection",
          |   "config": {
          |     "wallet": "walletAddress",
          |     "difficulty_factor": 10,
          |     "transaction_request_value": 10,
          |     "max_chunk_size": 10
          |   }
          |}
          |""".stripMargin
    }

    /**
     * Purpose: Load config from the pool server
     * Prerequisites: None.
     * Scenario: Mock pool to get test data from fetchConfig method.
     * Test Conditions:
     * * returned value is true (that means success)
     */
    "loadConfig" in {
      val pool = mock[Pool]
      when(pool.fetchConfig(any())).thenReturn(
        HttpResponse[Array[Byte]](
          """
            |{
            |   "wallet_address": "walletAddress",
            |   "pool_base_factor": 10,
            |   "reward": 10,
            |   "max_chunk_size": 10,
            |}
            |""".stripMargin.map(_.toByte).toArray, 200, Map[String, IndexedSeq[String]]()
        )
      )
      when(pool.walletAddress).thenCallRealMethod()
      when(pool.difficultyFactor).thenCallRealMethod()
      when(pool.transactionRequestsValue).thenCallRealMethod()
      when(pool.maxChunkSize).thenCallRealMethod()
      when(pool.loadConfig("pk")).thenCallRealMethod()

      pool.loadConfig("pk") mustBe true
    }
  }
}
