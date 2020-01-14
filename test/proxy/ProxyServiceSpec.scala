package proxy

import akka.util.ByteString
import org.scalatestplus.play._
import play.api.libs.Files.SingletonTemporaryFileCreator
import play.api.mvc.RawBuffer
import play.api.test._
import play.api.test.Helpers._


class ProxyServiceSpec extends PlaySpec {
  /** Check shares response body */
  "ProxyService getShareRequestBody" should {
    /**
     * Purpose: Check if a single share will correctly being refine.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: Pass a fake share to the getShareRequestBody method of ProxyService.
     * Test Conditions:
     * * size of response is 1
     * * response is correct
     */
    "return one prepared share in an iterable" in {
      val body: String =
        """
          |{
          |  "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
          |  "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
          |  "n": "0000000010C006CF",
          |  "d": 4196585670338033714759641235444284559441802073009721710293850518130743229130
          |}
          |""".stripMargin.replaceAll("\\s", "")
      val bytes: ByteString = ByteString(body)
      val fakeRequest = FakeRequest(POST, "/mining/share").withHeaders(("Content-Type", "")).withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = ProxyService.getShareRequestBody(fakeRequest)

      response.size mustBe 1
      response.foreach(item => item.replaceAll("\\s", "") mustBe
        """
          |{
          |  "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
          |  "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
          |  "nonce": "0000000010C006CF",
          |  "d": "4196585670338033714759641235444284559441802073009721710293850518130743229130"
          |}
          |""".stripMargin.replaceAll("\\s", "")
      )
    }

    /**
     * Purpose: Check if two shares will correctly being refine.
     * Prerequisites: Check test node and test pool server connections in test.conf.
     * Scenario: Pass two fake shares to the getShareRequestBody method of ProxyService.
     * Test Conditions:
     * * size of response is 2
     * * responses are correct
     */
    "return two prepared shares in an iterable" in {
      val body: String =
        """
          |[
          |   {
          |     "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
          |     "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
          |     "n": "0000000010C006CF",
          |     "d": 4196585670338033714759641235444284559441802073009721710293850518130743229130
          |   },
          |   {
          |     "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
          |     "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
          |     "n": "0000000010C006CF",
          |     "d": 4196585670338033714759641235444284559441802073009721710293850518130743229130
          |   }
          |]
          |""".stripMargin.replaceAll("\\s", "")
      val bytes: ByteString = ByteString(body)
      val fakeRequest = FakeRequest(POST, "/mining/share").withHeaders(("Content-Type", "")).withBody[RawBuffer](RawBuffer(bytes.size, SingletonTemporaryFileCreator, bytes))
      val response = ProxyService.getShareRequestBody(fakeRequest)

      response.size mustBe 2
      response.foreach(item => item.replaceAll("\\s", "") mustBe
        """
          |{
          |  "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
          |  "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
          |  "nonce": "0000000010C006CF",
          |  "d": "4196585670338033714759641235444284559441802073009721710293850518130743229130"
          |}
          |""".stripMargin.replaceAll("\\s", "")
      )
    }
  }
}
