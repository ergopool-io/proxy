package node

import helpers.{ConfigTrait, Helper}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.db.DBApi
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

class ProofTest extends PlaySpec with MockitoSugar with BeforeAndAfterAll with BeforeAndAfterEach with ConfigTrait {

  "Proof" should {
    /**
     * Purpose: valid proof which selects the right levels
     * Prerequisites: None.
     * Scenario: call proof with one valid proof
     * Test Conditions:
     *  return a vector with that valid proof
     */
    "return valid proof with appropriate levels" in {
      val proofBody =
        """
          |{
          |    "msgPreimage": "015c8d7001be3c3452b5649d558f1ae4a521f859d339cabe656b324560621076b45be8ebf05d9a9ac5716eb2d92bc0c7801a04374b7c31cca20746ca4cf803dc8ca8bc246898f68eb1522d2a64bcb17c5d9441f69d11721db4e1d0bbf669f76d85c7f959d2c3ad1700a7ef4308a13fc71e9a6240d5bde2a4fd3b9e426b1afe95e314868ce5c08e2e3d857f722180fab1031b3f850e2152ba542c831db7de64c95e7bc8facc1c957c05370c88f18707000000",
          |    "txProofs": [
          |      {
          |        "leaf": "b07abc60e9b35a766c76d2ef67c3a370261959baa5dde3cb667a969e2b9c2229",
          |        "levels": [
          |          "017ef5c857e9e28f8b241e4cbc55e958a9796b30eb49f2c92e81ef76a4fd5dae76",
          |          "00148e8878323df15835f336bfb3dd6fcd5af74f7e1922c0321e18defc00188643"
          |        ]
          |      },
          |      {
          |        "leaf": "13ab1d303b9ae24a01535af2e7983c28cf185618c574f32e6b06524a3cb98afd",
          |        "levels": [
          |          "00",
          |          "01e1c62dba0d18c08ecb919cac01d617d6909b38346b8f3e02eecc869c05322906"
          |        ]
          |      }
          |    ]
          |  }
          |""".stripMargin
      val proof = Proof(Helper.convertToJson(proofBody), "b07abc60e9b35a766c76d2ef67c3a370261959baa5dde3cb667a969e2b9c2229")
      proof.body mustBe Helper.convertToJson(
        """
          |{
          |    "msg_pre_image": "015c8d7001be3c3452b5649d558f1ae4a521f859d339cabe656b324560621076b45be8ebf05d9a9ac5716eb2d92bc0c7801a04374b7c31cca20746ca4cf803dc8ca8bc246898f68eb1522d2a64bcb17c5d9441f69d11721db4e1d0bbf669f76d85c7f959d2c3ad1700a7ef4308a13fc71e9a6240d5bde2a4fd3b9e426b1afe95e314868ce5c08e2e3d857f722180fab1031b3f850e2152ba542c831db7de64c95e7bc8facc1c957c05370c88f18707000000",
          |    "leaf": "b07abc60e9b35a766c76d2ef67c3a370261959baa5dde3cb667a969e2b9c2229",
          |    "levels": [
          |          "017ef5c857e9e28f8b241e4cbc55e958a9796b30eb49f2c92e81ef76a4fd5dae76",
          |          "00148e8878323df15835f336bfb3dd6fcd5af74f7e1922c0321e18defc00188643"
          |        ]
          |}
          |""".stripMargin)
    }

    /**
     * Purpose: null because transaction is not present in proof
     * Prerequisites: None.
     * Scenario: call proof with one invalid proof, tx does not exist in the proof
     * Test Conditions:
     *  return null
     */
    "return null, tx not present" in {
      val proofBody =
        """
          |{
          |    "msgPreimage": "015c8d7001be3c3452b5649d558f1ae4a521f859d339cabe656b324560621076b45be8ebf05d9a9ac5716eb2d92bc0c7801a04374b7c31cca20746ca4cf803dc8ca8bc246898f68eb1522d2a64bcb17c5d9441f69d11721db4e1d0bbf669f76d85c7f959d2c3ad1700a7ef4308a13fc71e9a6240d5bde2a4fd3b9e426b1afe95e314868ce5c08e2e3d857f722180fab1031b3f850e2152ba542c831db7de64c95e7bc8facc1c957c05370c88f18707000000",
          |    "txProofs": [
          |      {
          |        "leaf": "b07abc60e9b35a766c76d2ef67c3a370261959baa5dde3cb667a969e2b9c2229",
          |        "levels": [
          |          "017ef5c857e9e28f8b241e4cbc55e958a9796b30eb49f2c92e81ef76a4fd5dae76",
          |          "00148e8878323df15835f336bfb3dd6fcd5af74f7e1922c0321e18defc00188643"
          |        ]
          |      },
          |      {
          |        "leaf": "13ab1d303b9ae24a01535af2e7983c28cf185618c574f32e6b06524a3cb98afd",
          |        "levels": [
          |          "00",
          |          "01e1c62dba0d18c08ecb919cac01d617d6909b38346b8f3e02eecc869c05322906"
          |        ]
          |      }
          |    ]
          |  }
          |""".stripMargin
      val proof = Proof(Helper.convertToJson(proofBody), "some invalid transaction id")
      proof mustBe null
    }

    /**
     * Purpose: null because txProofs is empty
     * Prerequisites: None.
     * Scenario: call proof with empty proof, tx does not exist in the proof
     * Test Conditions:
     *  return null
     */
    "return null, txProofs empty" in {
      val proofBody =
        """
          |{
          |    "msgPreimage": "015c8d7001be3c3452b5649d558f1ae4a521f859d339cabe656b324560621076b45be8ebf05d9a9ac5716eb2d92bc0c7801a04374b7c31cca20746ca4cf803dc8ca8bc246898f68eb1522d2a64bcb17c5d9441f69d11721db4e1d0bbf669f76d85c7f959d2c3ad1700a7ef4308a13fc71e9a6240d5bde2a4fd3b9e426b1afe95e314868ce5c08e2e3d857f722180fab1031b3f850e2152ba542c831db7de64c95e7bc8facc1c957c05370c88f18707000000",
          |    "txProofs": [
          |    ]
          |  }
          |""".stripMargin
      val proof = Proof(Helper.convertToJson(proofBody), "b07abc60e9b35a766c76d2ef67c3a370261959baa5dde3cb667a969e2b9c2229")
      proof mustBe null
    }
  }
}
