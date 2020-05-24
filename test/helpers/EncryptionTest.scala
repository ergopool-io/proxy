package helpers

import javax.crypto.BadPaddingException
import org.scalatest.{BeforeAndAfterAll, PrivateMethodTester}
import org.scalatestplus.play.PlaySpec

class EncryptionTest extends PlaySpec with BeforeAndAfterAll with PrivateMethodTester {
  "Encryption" should {

    /**
     * Purpose: test encryption
     * Prerequisites: None.
     * Scenario: encrypt and then decrypt a message
     * Test Conditions:
     * * result must be the same as message
     */
    "decrypt message with appropriate key and secret" in {
      val message = "census erode want novel panther lens head hire else south leaf situate hill tiger metal maze amused short repeat also canvas skate outdoor video"
      val encryption = new Encryption("testKey", "testSecret")
      val encrypted = encryption.encrypt(message)
      val decrypted = encryption.decrypt(encrypted)
      decrypted mustBe message
    }

    /**
     * Purpose: test encryption
     * Prerequisites: None.
     * Scenario: encrypt and then decrypt with wrong key
     * Test Conditions:
     * * BadPaddingException must be raised
     */
    "decrypt message with wrong key" in {
      val message = "census erode want novel panther lens head hire else south leaf situate hill tiger metal maze amused short repeat also canvas skate outdoor video"
      var encryption = new Encryption("testKey", "testSecret")
      val encrypted = encryption.encrypt(message)
      encryption = new Encryption("testkey", "testSecret")
      try {
        encryption.decrypt(encrypted)
        fail()

      } catch {
        case _: BadPaddingException =>
      }
    }

    /**
     * Purpose: test encryption
     * Prerequisites: None.
     * Scenario: encrypt and then decrypt with wrong secret
     * Test Conditions:
     * * BadPaddingException must be raised
     */
    "decrypt message with wrong secret" in {
      val message = "census erode want novel panther lens head hire else south leaf situate hill tiger metal maze amused short repeat also canvas skate outdoor video"
      var encryption = new Encryption("testKey", "testSecret")
      val encrypted = encryption.encrypt(message)
      encryption = new Encryption("testKey", "testsecret")
      try {
        encryption.decrypt(encrypted)
        fail()

      } catch {
        case _: BadPaddingException =>
      }
    }
  }
}
