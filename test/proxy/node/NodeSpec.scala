package proxy.node

import org.scalatest.{BeforeAndAfterAll, PrivateMethodTester}
import org.scalatestplus.play.PlaySpec
import proxy.{Config, Mnemonic}
import testservers.{NodeServlets, TestNode, TestResponses}


class NodeSpec extends PlaySpec with BeforeAndAfterAll with PrivateMethodTester {
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
      NodeServlets.protectionAddress = "5Hg4a36kRJxyZpQBh4g5ConDLfFZFNAu2UvhuMydaLcqQwg5CySs1ptD3aFMHHHie5eZ6cNwW8" +
        "JWTTduodU5U4eAVvRkV3QJVExpUZaxzv5grXsx8At4yAcyvtNb1vYQtf5Zo68qAGKp4sTDYqEV1M2kiH7kdBCzHzLYnxCMEYJJ4qA45MSqKQV"
      val mnemonicValueSetter = PrivateMethod[Unit]('setValue)
      Mnemonic invokePrivate mnemonicValueSetter("vapor ice mind spray humble chicken adapt all brief faith pilot " +
        "wool million bubble spy robust trend elevator quarter sun hair all share acquire")
      Node.pk = "03dafb7b05c4acadd8bce096ecacc3b7d24c86715b8d533a9182f0fc135558c921"
      NodeServlets.walletAddresses = Vector[String]("3address1")
      Mnemonic.createAddress()
      NodeServlets.unspentBoxes = TestResponses.unspentBoxes
      Config.transactionRequestsValue = 67500000000L
      Node.createProtectionScript()

      Node.fetchUnspentBoxes()
      Node.protectedUnspentBoxes.size mustBe 1
    }
  }
}
