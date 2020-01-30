package proxy.node

import akka.util.ByteString
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.mvc.RawBuffer
import play.api.test.FakeRequest
import play.api.test.Helpers._
import proxy.Config
import testservers.{NodeServlets, TestNode, TestPoolServer}

import scala.util.{Failure, Try}

class MiningCandidateSpec extends PlaySpec with BeforeAndAfterAll {
  val testNodeConnection: String = Config.nodeConnection
  val testPoolServerConnection: String = Config.poolConnection

  val node: TestNode = new TestNode(testNodeConnection.split(":").last.toInt)
  val pool: TestPoolServer = new TestPoolServer(testPoolServerConnection.split(":").last.toInt)

  node.startServer()
  pool.startServer()

  NodeServlets.proof = "null"

  override def afterAll(): Unit = {
    node.stopServer()
    pool.stopServer()

    super.afterAll()
  }

  /** Check MiningCandidate on fail scenarios */
  "MiningCandidate getShareRequestBody fail scenarios" should {
    /**
     * Purpose: Check if exception will be thrown if generating transaction fails.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: Set test node to fail transaction and send /mining/candidate and pass the response to MiningCandidate.
     * Test Conditions:
     * * exception is thrown
     */
    "throw exception on failure in transaction generation" in {
      NodeServlets.msg = "qwertyuiopasdfghjklzxcvbnm"
      val bytes: ByteString = ByteString("")
      val fakeRequest = FakeRequest(GET, "/mining/candidate").withHeaders(("Content-Type", "")).withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = Node.sendRequest("/mining/candidate", fakeRequest)

      NodeServlets.failTransaction = true
      val triedStatement = Try {
        new MiningCandidate(response).getResponse
      }

      NodeServlets.failTransaction = false

      triedStatement match {
        case Failure(_) =>
        case _ =>
          NodeServlets.failTransaction = false
          fail("Expected to throw exception but didn't")
      }
    }
  }
}
