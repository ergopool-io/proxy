package controllers.testservers

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHandler

import helpers.Helper

class TestPoolServer(port: Int) extends TestJettyServer {
  override val serverPort: Int = port
  override val serverName: String = "Test Pool Server"

  override protected val server: Server = createServer()

  override protected val handler: ServletHandler = new ServletHandler()

  server.setHandler(handler)

  handler.addServletWithMapping(classOf[PoolServerServlets.ConfigServlet], "/api/config/value.json/")
  handler.addServletWithMapping(classOf[PoolServerServlets.TransactionServlet], "/api/transaction.json/")
  handler.addServletWithMapping(classOf[PoolServerServlets.InternalServerErrorServlet], "/api/share.json/")
  handler.addServletWithMapping(classOf[PoolServerServlets.HeaderServlet], "/api/header.json/")
}

object PoolServerServlets {
  var gotSolution: Boolean = false
  var gotProof: Boolean = false

  class TransactionServlet extends HttpServlet {
    val reqBodyCheck: String =
      """
        |{
        |  "pk": "0278011ec0cf5feb92d61adb51dcb75876627ace6fd9446ab4cabc5313ab7b39a7",
        |  "transaction": {
        |      "id": "2ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
        |      "inputs": [
        |        {
        |          "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
        |          "spendingProof": {
        |            "proofBytes": "4ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd1173ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd1173ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
        |            "extension": {
        |              "1": "a2aed72ff1b139f35d1ad2938cb44c9848a34d4dcfd6d8ab717ebde40a7304f2541cf628ffc8b5c496e6161eba3f169c6dd440704b1719e0"
        |            }
        |          }
        |        }
        |      ],
        |      "dataInputs": [
        |        {
        |          "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117"
        |        }
        |      ],
        |      "outputs": [
        |        {
        |          "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
        |          "value": 147,
        |          "ergoTree": "0008cd0336100ef59ced80ba5f89c4178ebd57b6c1dd0f3d135ee1db9f62fc634d637041",
        |          "creationHeight": 9149,
        |          "assets": [
        |            {
        |              "tokenId": "4ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
        |              "amount": 1000
        |            }
        |          ],
        |          "additionalRegisters": {
        |            "R4": "100204a00b08cd0336100ef59ced80ba5f89c4178ebd57b6c1dd0f3d135ee1db9f62fc634d637041ea02d192a39a8cc7a70173007301"
        |          }
        |        }
        |      ],
        |      "size": 0
        |    }
        |}
        |""".stripMargin.replaceAll("\\s", "")
    override protected def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit = {
      val body: String = Helper.readHttpServletRequestBody(request).replaceAll("\\s", "")
      response.setContentType("application/json")
      if (body == reqBodyCheck) {
        response.setStatus(HttpServletResponse.SC_OK)
        response.getWriter.print("{\"success\": true}")
      }
      else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        response.getWriter.print("{\"success\": false}")
      }
    }
  }

  class ConfigServlet extends HttpServlet {
    override protected def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
      response.setContentType("application/json")
      response.setStatus(HttpServletResponse.SC_OK)
      response.getWriter.print(
        s"""
           |{
           |    "reward": 67500000000,
           |    "wallet_address": "3WvrVTCPJ1keSdtqNL5ayzQ62MmTNz4Rxq7vsjcXgLJBwZkvHrGa",
           |    "pool_difficulty_factor": 10
           |}
           |""".stripMargin)
    }
  }

  class SolutionServlet extends HttpServlet {
    val reqBodyCheck: String =
      """
        |{
        |  "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
        |  "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
        |  "nonce": "0000000010C006CF",
        |  "d": "4196585670338033714759641235444284559441802073009721710293850518130743229130"
        |}
        |""".stripMargin.replaceAll("\\s", "")
    override protected def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit = {
      val body: String = Helper.readHttpServletRequestBody(request).replaceAll("\\s", "")
      response.setContentType("application/json")
      if (body == reqBodyCheck) {
        gotSolution = true
        response.setStatus(HttpServletResponse.SC_OK)
        response.getWriter.print("{\"success\": true}")
      }
      else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        response.getWriter.print("{\"success\": false}")
      }
    }
  }

  class HeaderServlet extends HttpServlet {
    val reqBodyCheck: String =
      """
        |{
        |   "pk": "0278011ec0cf5feb92d61adb51dcb75876627ace6fd9446ab4cabc5313ab7b39a7",
        |   "msg_pre_image": "01fb9e35f8a73c128b73e8fde5c108228060d68f11a69359ee0fb9bfd84e7ecde6d19957ccbbe75b075b3baf1cac6126b6e80b5770258f4cec29fbde92337faeec74c851610658a40f5ae74aa3a4babd5751bd827a6ccc1fe069468ef487cb90a8c452f6f90ab0b6c818f19b5d17befd85de199d533893a359eb25e7804c8b5d7514d784c8e0e52dabae6e89a9d6ed9c84388b228e7cdee09462488c636a87931d656eb8b40f82a507008ccacbee05000000",
        |   "leaf": "642c15c62553edd8fd9af9a6f754f3c7a6c03faacd0c9b9d5b7d11052c6c6fe8",
        |   "levels": [
        |      "0139b79af823a92aa72ced2c6d9e7f7f4687de5b5af7fab0ad205d3e54bda3f3ae"
        |   ]
        |}
        |""".stripMargin.replaceAll("\\s", "")
    override protected def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit = {
      val body: String = Helper.readHttpServletRequestBody(request).replaceAll("\\s", "")
      response.setContentType("application/json")
      if (body == reqBodyCheck) {
        gotProof = true
        response.setStatus(HttpServletResponse.SC_OK)
        response.getWriter.print("{\"success\": true}")
      }
      else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        response.getWriter.print("{\"success\": false}")
      }
    }
  }

  class InternalServerErrorServlet extends HttpServlet {
    override protected def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit = {
      response.setContentType("application/json")
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      response.getWriter.print("{}")
    }

    override protected def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
      response.setContentType("application/json")
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      response.getWriter.print("{}")
    }
  }
}
