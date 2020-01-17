package proxy.node

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import proxy.Config
import testservers.{NodeServlets, TestNode}


class NodeSpec extends PlaySpec with BeforeAndAfterAll {
  val testNodeConnection: String = Config.nodeConnection

  val node: TestNode = new TestNode(testNodeConnection.split(":").last.toInt)

  node.startServer()

  override def afterAll(): Unit = {
    node.stopServer()

    super.afterAll()
  }

  /** Check MiningCandidate on fail scenarios */
  "Node createProtectionScript creates protection and saves the address" should {
    /**
     * Purpose: Check creation of protection script scenario.
     * Prerequisites: Check test node connection in test.conf.
     * Scenario: Call method to create and save protection address.
     * Test Conditions:
     * * protection address is right
     */
    "throw exception on failure in transaction generation" in {
      Node.createProtectionScript()
      Node.lastProtectionAddress mustBe NodeServlets.protectionAddress
    }
  }
}
