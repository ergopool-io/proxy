package testservers

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHandler
import helpers.Helper

class TestNode(port: Int) extends TestJettyServer {
  override val serverPort: Int = port
  override val serverName: String = "Test Node"

  override protected val server: Server = createServer()

  override protected val handler: ServletHandler = new ServletHandler()

  server.setHandler(handler)

  handler.addServletWithMapping(classOf[NodeServlets.ProxyServlet], "/test/proxy")
  handler.addServletWithMapping(classOf[NodeServlets.MiningCandidateServlet], "/mining/candidate")
  handler.addServletWithMapping(classOf[NodeServlets.MiningSolutionServlet], "/mining/solution")
  handler.addServletWithMapping(classOf[NodeServlets.MiningCandidateWithTxsServlet], "/mining/candidateWithTxs")
  handler.addServletWithMapping(classOf[NodeServlets.WalletTransactionGenerateServlet], "/wallet/transaction/generate")
  handler.addServletWithMapping(classOf[NodeServlets.SwaggerConfigServlet], "/api-docs/swagger.conf")
  handler.addServletWithMapping(classOf[NodeServlets.InfoServlet], "/info")
  handler.addServletWithMapping(classOf[NodeServlets.P2SAddress], "/script/p2sAddress")
  handler.addServletWithMapping(classOf[NodeServlets.UTXOByIdBinaryServlet], "/utxo/byIdBinary/*")
  handler.addServletWithMapping(classOf[NodeServlets.WalletTransactionByIdServlet], "/wallet/transactionById")
  handler.addServletWithMapping(classOf[NodeServlets.WalletBoxesUnspentServlet], "/wallet/boxes/unspent")
  handler.addServletWithMapping(classOf[NodeServlets.WalletDeriveKeyServlet], "/wallet/deriveKey")
}

object NodeServlets {
  var proof: String = "null"
  var proofCreated: Boolean = false
  var msg: String = ""
  var failTransaction: Boolean = false
  val protectionAddress: String = "3WwbzW6u8hKWBcL1W7kNVMr25s2UHfSBnYtwSHvrRQt7DdPuoXrt"
  val transactionResponse: String =
    """
      |{
      |  "id": "2ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
      |  "inputs": [
      |    {
      |      "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
      |      "spendingProof": {
      |        "proofBytes": "4ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd1173ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd1173ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
      |        "extension": {
      |          "1": "a2aed72ff1b139f35d1ad2938cb44c9848a34d4dcfd6d8ab717ebde40a7304f2541cf628ffc8b5c496e6161eba3f169c6dd440704b1719e0"
      |        }
      |      }
      |    }
      |  ],
      |  "dataInputs": [
      |    {
      |      "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117"
      |    }
      |  ],
      |  "outputs": [
      |    {
      |      "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
      |      "value": 147,
      |      "ergoTree": "0008cd0336100ef59ced80ba5f89c4178ebd57b6c1dd0f3d135ee1db9f62fc634d637041",
      |      "creationHeight": 9149,
      |      "assets": [
      |        {
      |          "tokenId": "4ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
      |          "amount": 1000
      |        }
      |      ],
      |      "additionalRegisters": {
      |        "R4": "100204a00b08cd0336100ef59ced80ba5f89c4178ebd57b6c1dd0f3d135ee1db9f62fc634d637041ea02d192a39a8cc7a70173007301"
      |      }
      |    }
      |  ],
      |  "size": 0
      |}
      |""".stripMargin
  var unspentBoxes: String =
    s"""
      |[
      |  {
      |    "box": {
      |      "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
      |      "value": 60500000000
      |    },
      |    "address": "$protectionAddress"
      |  },
      |  {
      |    "box": {
      |      "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
      |      "value": 147
      |    },
      |    "address": "another_address"
      |  }
      |]
      |""".stripMargin

  class ProxyServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print("{\"success\": true}")
    }

    override protected def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print("{\"success\": true}")
    }
  }

  class MiningCandidateServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print(
        s"""
           |{
           |  "msg": "$msg",
           |  "b": 748014723576678314041035877227113663879264849498014394977645987,
           |  "pk": "0278011ec0cf5feb92d61adb51dcb75876627ace6fd9446ab4cabc5313ab7b39a7",
           |  "proof": $proof
           |}
           |""".stripMargin)
    }
  }

  class MiningSolutionServlet extends HttpServlet {
    val reqBodyCheck: String =
      """
        |{
        |  "pk": "0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5",
        |  "w": "0366ea253123dfdb8d6d9ca2cb9ea98629e8f34015b1e4ba942b1d88badfcc6a12",
        |  "n": "0000000010C006CF",
        |  "d": 4196585670338033714759641235444284559441802073009721710293850518130743229130e0
        |}
        |""".stripMargin.replaceAll("\\s", "")
    override protected def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      val body: String = Helper.readHttpServletRequestBody(req).replaceAll("\\s", "")

      resp.setContentType("application/json")
      if (body == reqBodyCheck) {
        resp.setStatus(HttpServletResponse.SC_OK)
        resp.getWriter.print("{\"success\": true}")
      }
      else {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        resp.getWriter.print(
          """
            |{
            |  "error": 500,
            |  "reason": "Internal server error",
            |  "detail": "string"
            |}
            |""".stripMargin)
      }
    }
  }

  class MiningCandidateWithTxsServlet extends HttpServlet {
    val reqBodyCheckTwoTransaction: String =
      s"""
         |[
         |  {
         |    "transaction": $transactionResponse,
         |    "cost": 50000
         |  },
         |  {
         |    "transaction": $transactionResponse,
         |    "cost": 50000
         |  }
         |]
         |""".stripMargin.replaceAll("\\s", "")
    val reqBodyCheckOneTransaction: String =
      s"""
         |[
         |  {
         |    "transaction": $transactionResponse,
         |    "cost": 50000
         |  }
         |]
         |""".stripMargin.replaceAll("\\s", "")
    override protected def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      val body: String = Helper.readHttpServletRequestBody(req).replaceAll("\\s", "")
      resp.setContentType("application/json")
      if (body == reqBodyCheckOneTransaction || body == reqBodyCheckTwoTransaction) {
        proof =
          """
            |{
            |  "msgPreimage" : "01fb9e35f8a73c128b73e8fde5c108228060d68f11a69359ee0fb9bfd84e7ecde6d19957ccbbe75b075b3baf1cac6126b6e80b5770258f4cec29fbde92337faeec74c851610658a40f5ae74aa3a4babd5751bd827a6ccc1fe069468ef487cb90a8c452f6f90ab0b6c818f19b5d17befd85de199d533893a359eb25e7804c8b5d7514d784c8e0e52dabae6e89a9d6ed9c84388b228e7cdee09462488c636a87931d656eb8b40f82a507008ccacbee05000000",
            |  "txProofs" : [{
            |    "leaf" : "642c15c62553edd8fd9af9a6f754f3c7a6c03faacd0c9b9d5b7d11052c6c6fe8",
            |    "levels" : [
            |      "0139b79af823a92aa72ced2c6d9e7f7f4687de5b5af7fab0ad205d3e54bda3f3ae"
            |    ]
            |  }]
            |}
            |""".stripMargin
        proofCreated = true
        resp.setStatus(HttpServletResponse.SC_OK)
        resp.getWriter.print(
          s"""
             |{
             |  "msg" : "$msg",
             |  "b" : 748014723576678314041035877227113663879264849498014394977645987,
             |  "pk" : "0278011ec0cf5feb92d61adb51dcb75876627ace6fd9446ab4cabc5313ab7b39a7",
             |  "proof" : $proof
             |}
             |""".stripMargin)
      }
      else {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        resp.getWriter.print("{\"success\": false}")
      }
    }
  }

  class WalletTransactionGenerateServlet extends HttpServlet {
    override protected def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      if (!failTransaction) {
        resp.setStatus(HttpServletResponse.SC_OK)
        resp.getWriter.print(transactionResponse)
      }
      else {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        resp.getWriter.print(
          """
            |{
            |  "error": 500,
            |  "reason": "Internal server error",
            |  "detail": "string"
            |}
            |""".stripMargin)
      }
    }
  }

  class SwaggerConfigServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      val testSwaggerConfig: String =
        """
          |openapi: "3.0.2"
          |
          |info:
          |  version: "3.1.2"
          |  title: Ergo Node API
          |  description: API docs for Ergo Node. Models are shared between all Ergo products
          |  contact:
          |    name: Ergo Platform Team
          |    email: ergoplatform@protonmail.com
          |    url: https://ergoplatform.org
          |  license:
          |    name: CC0 1.0 Universal
          |    url: https://raw.githubusercontent.com/ergoplatform/ergo/master/LICENSE
          |paths:
          |  /info:
          |    get:
          |      tags:
          |      - info
          |      summary: Get the information about the Node
          |      operationId: getNodeInfo
          |      responses:
          |        200:
          |          description: Node info object
          |          content:
          |            application/json:
          |              schema:
          |                $ref: '#/components/schemas/NodeInfo'
          |        default:
          |          description: Error
          |          content:
          |            application/json:
          |              schema:
          |                $ref: '#/components/schemas/ApiError'
          |components:
          |  securitySchemes:
          |    ApiKeyAuth:
          |      type: apiKey
          |      in: header
          |      name: api_key
          |
          |  schemas:
          |    # Objects
          |    ExternalCandidateBlock:
          |      description: Candidate block info for external miner
          |      type: object
          |      required:
          |      - msg
          |      - b
          |      - pk
          |      - pb
          |      properties:
          |        msg:
          |          type: string
          |          description: Base16-encoded block bytes without pow
          |          example: '0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5'
          |        b:
          |          type: integer
          |          example: 987654321
          |        b:
          |          type: integer
          |          example: 9876543210
          |        pk:
          |          type: string
          |          description: Base16-encoded public key
          |          example: '0350e25cee8562697d55275c96bb01b34228f9bd68fd9933f2a25ff195526864f5'
          |
          |    ApiError:
          |      type: object
          |      required:
          |      - error
          |      - reason
          |      - detail
          |      properties:
          |        error:
          |          type: integer
          |          description: Error code
          |          example: 500
          |        reason:
          |          type: string
          |          description: String error code
          |          example: 'Internal server error'
          |        detail:
          |          type: string
          |          nullable: true
          |          description: Detailed error description
          |
          |paths:
          |  /mining/candidate:
          |    get:
          |      security:
          |        - ApiKeyAuth: [api_key]
          |      summary: Request block candidate
          |      operationId: miningRequestBlockCandidate
          |      tags:
          |        - mining
          |      responses:
          |        '200':
          |          description: External candidate
          |          content:
          |            application/json:
          |              schema:
          |                $ref: '#/components/schemas/ExternalCandidateBlock'
          |        default:
          |          description: Error
          |          content:
          |            application/json:
          |              schema:
          |                $ref: '#/components/schemas/ApiError'
          |
          |  /mining/rewardAddress:
          |    get:
          |      security:
          |        - ApiKeyAuth: [api_key]
          |      summary: Read miner reward address
          |      operationId: miningReadMinerRewardAddress
          |      tags:
          |        - mining
          |      responses:
          |        '200':
          |          description: External candidate
          |          content:
          |            application/json:
          |              schema:
          |                type: object
          |                required:
          |                  - rewardAddress
          |        default:
          |          description: Error
          |          content:
          |            application/json:
          |              schema:
          |                $ref: '#/components/schemas/ApiError'
          |
          |  /mining/solution:
          |    post:
          |      security:
          |        - ApiKeyAuth: [api_key]
          |      summary: Submit solution for current candidate
          |      operationId: miningSubmitSolution
          |      tags:
          |        - mining
          |      requestBody:
          |        required: true
          |        content:
          |          application/json:
          |            schema:
          |              $ref: '#/components/schemas/PowSolutions'
          |      responses:
          |        '200':
          |          description: Solution is valid
          |        '400':
          |          description: Solution is invalid
          |          content:
          |            application/json:
          |              schema:
          |                $ref: '#/components/schemas/ApiError'
          |        default:
          |          description: Error
          |          content:
          |            application/json:
          |              schema:
          |                $ref: '#/components/schemas/ApiError'
          |""".stripMargin
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print(testSwaggerConfig)
    }
  }

  class InfoServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print("{}")
    }
  }

  class P2SAddress extends HttpServlet {
    override protected def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print(
        s"""
          |{
          |  "address": "$protectionAddress"
          |}
          |""".stripMargin)
    }
  }

  class UTXOByIdBinaryServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print(
        """
          |{
          |  "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
          |  "bytes": "4ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117"
          |}
          |""".stripMargin)
    }
  }

  class WalletTransactionByIdServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print(
        """
          |[
          |  {
          |    "id": "2ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117"
          |  }
          |]
          |""".stripMargin)
    }
  }

  class WalletBoxesUnspentServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print(unspentBoxes)
    }
  }

  class WalletDeriveKeyServlet extends HttpServlet {
    override protected def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print(
        """
          |{
          |  "address": "3WwbzW6u8hKWBcL1W7kNVMr25s2UHfSBnYtwSHvrRQt7DdPuoXrt"
          |}
          |""".stripMargin)
    }
  }
}