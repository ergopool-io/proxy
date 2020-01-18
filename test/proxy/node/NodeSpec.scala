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

  "Node createProtectionScript" should {
    /**
     * Purpose: Check creation of protection script scenario.
     * Prerequisites: Check test node connection in test.conf.
     * Scenario: Call method to create and save protection address.
     * Test Conditions:
     * * protection address is right
     */
    "creates protection and saves the address" in {
      Node.createProtectionScript()
      Node.lastProtectionAddress mustBe NodeServlets.protectionAddress
    }
  }

  "Node fetchUnspentBoxes" should {
    /**
     * Purpose: Check just boxes with protection address are fetched.
     * Prerequisites: Check test node connection in test.conf.
     * Scenario: Call method to fetch and save unspent boxes with the node protection address.
     * Test Conditions:
     * * there is one box in the vector
     */
    "fetch unspent boxes with protection address" in {
      Node.fetchUnspentBoxes()
      Node.unspentBoxes.size mustBe 1
    }
  }
}
