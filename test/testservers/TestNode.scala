package testservers

import helpers.Helper
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHandler

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
  handler.addServletWithMapping(classOf[NodeServlets.WalletBoxesUnspentServlet], "/wallet/boxes/unspent")
  handler.addServletWithMapping(classOf[NodeServlets.WalletDeriveKeyServlet], "/wallet/deriveKey")
  handler.addServletWithMapping(classOf[NodeServlets.WalletAddressesServlet], "/wallet/addresses")
  handler.addServletWithMapping(classOf[NodeServlets.SwaggerConfigServlet], "/api-docs/swagger.conf")
  handler.addServletWithMapping(classOf[NodeServlets.InfoServlet], "/info")
  handler.addServletWithMapping(classOf[NodeServlets.P2SAddress], "/script/p2sAddress")
  handler.addServletWithMapping(classOf[NodeServlets.UTXOByIdBinaryServlet], "/utxo/byIdBinary/*")
  handler.addServletWithMapping(classOf[NodeServlets.UTXOByIdServlet], "/utxo/byId/*")
  handler.addServletWithMapping(classOf[NodeServlets.WalletTransactionByIdServlet], "/wallet/transactionById")
  handler.addServletWithMapping(classOf[NodeServlets.Last10Blocks], "/blocks/lastHeaders/10")
}

object NodeServlets {
  var proof: String = "null"
  var proofCreated: Boolean = false
  var msg: String = ""
  var failTransaction: Boolean = false
  var protectionAddress: String = "3WwbzW6u8hKWBcL1W7kNVMr25s2UHfSBnYtwSHvrRQt7DdPuoXrt"
  var walletAddresses: Vector[String] = Vector[String]("3WwbzW6u8hKWBcL1W7kNVMr25s2UHfSBnYtwSHvrRQt7DdPuoXrt")
  var miningCandidate: String =
    s"""
      |{
      |  "msg": "$msg",
      |  "b": 748014723576678314041035877227113663879264849498014394977645987,
      |  "pk": "0278011ec0cf5feb92d61adb51dcb75876627ace6fd9446ab4cabc5313ab7b39a7",
      |  "proof": $proof
      |}
      |""".stripMargin
  var transactionResponse: String =
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
      resp.getWriter.print(miningCandidate)
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
        val body = Helper.convertToJson(IOUtils.toString(req.getReader))
        val boxes = body.hcursor.downField("inputsRaw").as[Vector[String]].getOrElse(Vector())
        if (boxes.isEmpty) {
          resp.setStatus(HttpServletResponse.SC_OK)
          resp.getWriter.print(transactionResponse)

        } else {
          val transactionResponse: String =
            s"""
               |{
               |  "id": "2ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
               |  "inputs": [
               |    {
               |      "boxId": "${boxes.apply(0)}",
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
          resp.setStatus(HttpServletResponse.SC_OK)
          resp.getWriter.print(transactionResponse)

        }
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
      resp.getWriter.print(
        """
          |{
          |  "name": "my-node-1",
          |  "appVersion": "0.0.1",
          |  "fullHeight": 667,
          |  "headersHeight": 667,
          |  "bestFullHeaderId": "3ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
          |  "previousFullHeaderId": "3ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
          |  "bestHeaderId": "3ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
          |  "stateRoot": "dab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
          |  "stateType": "digest",
          |  "stateVersion": "fab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
          |  "isMining": true,
          |  "peersCount": 327,
          |  "unconfirmedCount": 327,
          |  "difficulty": 667,
          |  "currentTime": 1524143059077,
          |  "launchTime": 1524143059077,
          |  "headersScore": 0,
          |  "fullBlocksScore": 0,
          |  "genesisBlockId": "3ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
          |  "parameters": {
          |    "height": 667,
          |    "storageFeeFactor": 100000,
          |    "minValuePerByte": 360,
          |    "maxBlockSize": 1048576,
          |    "maxBlockCost": 104876,
          |    "blockVersion": 2,
          |    "tokenAccessCost": 100,
          |    "inputCost": 100,
          |    "dataInputCost": 100,
          |    "outputCost": 100
          |  }
          |}
          |""".stripMargin)
    }
  }

  class P2SAddress extends HttpServlet {
    override protected def doPost(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      val body: String = IOUtils.toString(req.getReader)
      if (body.contains("3WyRt8MCd1XfZnWoPXdrngt9yb2BgdxRtM8RQz7btQSB32XF7sVf")) {
        resp.getWriter.print(
          s"""
             |{
             |  "address": "5Hg4a36kQGxB6zki3oCYbSfL3yfmhGCc4yYfRUwEdgqMWPbY57TAZozkvVunEsUmb5vgRU6iiFQzJkpaR8PJ7hfzbj8NNTf5Rmf2ou9LrmafJDD1ewX9YjqgGHWgJrpy2sLapEBR1sms6moJxH424dTmE7T6yuX1FW6PMFvExXb9XpsQ2fMWHdu"
             |}
             |""".stripMargin)

      } else {
        resp.getWriter.print(
          s"""
             |{
             |  "address": "$protectionAddress"
             |}
             |""".stripMargin)
      }
    }
  }

  class UTXOByIdBinaryServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      val url = req.getRequestURL.toString
      val last = url.split("/").last
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print(
        s"""
           |{
           |  "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
           |  "bytes": "4${last.slice(1, last.length)}"
           |}
           |""".stripMargin)
    }
  }

  class UTXOByIdServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      if (!failTransaction) {
        val url = req.getRequestURL.toString
        val last = url.split("/").last
        resp.setContentType("application/json")
        resp.setStatus(HttpServletResponse.SC_OK)
        resp.getWriter.print(
          s"""
             |{
             |  "boxId": "1ab9da11fc216660e974842cc3b7705e62ebb9e0bf5ff78e53f9cd40abadd117",
             |  "bytes": "4${last.slice(1, last.length)}"
             |}
             |""".stripMargin)
      } else {
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

  class WalletTransactionByIdServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      if (!failTransaction) {
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

      } else {
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
      val body = Helper.convertToJson(IOUtils.toString(req.getReader))
      if (body.hcursor.downField("derivationPath").as[String].getOrElse("") == "m") {
        resp.getWriter.print(
          s"""
             |{
             |   "address": ${if (walletAddresses.nonEmpty) s""""${walletAddresses.apply(0)}"""" else ""}
             |}
             |""".stripMargin)

      } else {
        resp.getWriter.print(
          s"""
             |{
             |   "address": "some other address"
             |}
             |""".stripMargin)

      }
    }

  }

  class WalletAddressesServlet extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print(
        s"""
           |[
           |   ${if (walletAddresses.nonEmpty) walletAddresses.map(f => s""""$f"""").mkString(",") else ""}
           |]
           |""".stripMargin)
    }
  }

  class Last10Blocks extends HttpServlet {
    override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
      resp.setContentType("application/json")
      resp.setStatus(HttpServletResponse.SC_OK)
      resp.getWriter.print(
        s"""
           |[
           |  {
           |    "extensionId": "fe34cc0db455532a89b319da2bbb22e9d2be3cf8e174ff6b349bfa621bb64f94",
           |    "difficulty": "236433440768",
           |    "votes": "000000",
           |    "timestamp": 1582991327440,
           |    "size": 281,
           |    "stateRoot": "f467fd5d170fbfe497266af69ef9130ede8fd7488b89501b4f7188f3f5e3f77814",
           |    "height": 115484,
           |    "nBits": 87493768,
           |    "version": 1,
           |    "id": "7e117202006efae8ad0254eadb429bf87e06998f23f8c12d7115072921f815fc",
           |    "adProofsRoot": "25993f5c6fcc54bf24153cb5334f78fab3b56e5373d5d814b0eb21e756381074",
           |    "transactionsRoot": "d7a88c6ed18e21165a6121d4f9ca9cd8822726feeede0c738e8fd333b24b4565",
           |    "extensionHash": "28d42f451baa82f5425e393e6112dd6cfeab844ecb5fc31b0dfc2ebb00fb60bc",
           |    "powSolutions": {
           |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
           |      "w": "035ee1b7da8bb77f592598c4d7be06ca7b430b4e3caac9d1b8b9d6c9c82e709c7f",
           |      "n": "0000051663515de4",
           |      "d": 3.005531905324926e+65
           |    },
           |    "adProofsId": "13c639cdf6cb78c7feafa988398d15921b8db7e7dbc645fedc931d7a59d098aa",
           |    "transactionsId": "96f267b48497c4ae91e421e28a2a8dff3da934261d616714e7fa0ce910c1a527",
           |    "parentId": "4a41d234f45a81cae11ef76a6ef088062d20f915a78c6edb29484ffeb4077d16"
           |  },
           |  {
           |    "extensionId": "4adfea14e72dc1f09c49b404a1107ba91509fc31efdcd99c24703de1cffbe26d",
           |    "difficulty": "236433440768",
           |    "votes": "000000",
           |    "timestamp": 1582999033356,
           |    "size": 282,
           |    "stateRoot": "4b7982b07cfda249f51593de6c9eb07f6fdb7937b19e9581c1c31a92075e05f214",
           |    "height": 115485,
           |    "nBits": 87493768,
           |    "version": 1,
           |    "id": "b10ffe360063d53aeb1c12438c1f95404bfe9430b25b6bd6e9e17fce46374452",
           |    "adProofsRoot": "487ee4b705423607ae77f341cbe801e9c87b117c7561f9e8dc0e2b5900a4a50f",
           |    "transactionsRoot": "73de2180b1500b50436d83583dc3aa94a2d99e2bfb739766b91f00e0c26b84e4",
           |    "extensionHash": "28d42f451baa82f5425e393e6112dd6cfeab844ecb5fc31b0dfc2ebb00fb60bc",
           |    "powSolutions": {
           |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
           |      "w": "02d7156e77d1edcfe4a1604f1df2c30b83d044fa2ffe93793babd37fe845483671",
           |      "n": "00000530167015d3",
           |      "d": 3.632691919608881e+65
           |    },
           |    "adProofsId": "712e6f963a757d17c0f0d2710c42dda69e793390f2ee340b6c71abdf3973c973",
           |    "transactionsId": "118ef8de0743d52f89ee83a83b730dd1952596b9d7cb09715739a0ab99269fb2",
           |    "parentId": "7e117202006efae8ad0254eadb429bf87e06998f23f8c12d7115072921f815fc"
           |  },
           |  {
           |    "extensionId": "0ade205a6e92241702c9a3364edb65154d453740e3ac6fdbd96c7bbbb5e3ca2c",
           |    "difficulty": "236433440768",
           |    "votes": "000000",
           |    "timestamp": 1583002234071,
           |    "size": 281,
           |    "stateRoot": "6e977011073a5c76461460726c109b323d55ead3b977ebd7e3d70a6ea014baa814",
           |    "height": 115486,
           |    "nBits": 87493768,
           |    "version": 1,
           |    "id": "afa1b7f536f175d6a60379f8427588b75ebe47d3fd4af8c244e74e26d8f3ec5e",
           |    "adProofsRoot": "9b6ad8c19f100197c915415f21953b09400c100fbb1b311a48463e071ec215fb",
           |    "transactionsRoot": "1906820bf419963c54a4b2ed0b0d2b7ed529acb73b44420874ec2dc0fea0e777",
           |    "extensionHash": "28d42f451baa82f5425e393e6112dd6cfeab844ecb5fc31b0dfc2ebb00fb60bc",
           |    "powSolutions": {
           |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
           |      "w": "03e843e013de81c4cae1ebcc1a43ef4fc4bdc85490404d029cb0a13fdcbb5ee618",
           |      "n": "0000057949af698b",
           |      "d": 1.44070243307032e+65
           |    },
           |    "adProofsId": "05252ad4adf425235bdbfb15f9b81f16e7f44121d744ac5265caba5d7c4bb329",
           |    "transactionsId": "1d8b32a9e86d224c8e1f81d818490ee267681eb9830d97bf8eab9d96a21764dd",
           |    "parentId": "b10ffe360063d53aeb1c12438c1f95404bfe9430b25b6bd6e9e17fce46374452"
           |  },
           |  {
           |    "extensionId": "5408d5807ef0cc4490008bfbfefb82aa155b5c20a1f9997aed5f7c172149a6fe",
           |    "difficulty": "236433440768",
           |    "votes": "000000",
           |    "timestamp": 1583011340469,
           |    "size": 281,
           |    "stateRoot": "3534efb18c6532c773b63d279e56974cb6068e08f5044fbbe3e0fc02bf45ede414",
           |    "height": 115487,
           |    "nBits": 87493768,
           |    "version": 1,
           |    "id": "c2ad321d5f7a03d478838c450a52cd352721569310b1f94339b0d88070e6f736",
           |    "adProofsRoot": "5d78c72cee068e0bcf8ef2b3f595c1594c20cdf180d24b00b31486d39339acc4",
           |    "transactionsRoot": "e246b4976301c9adf0d56424077f1c1890e0dd9ebdc95aa3b7548afac8e229e9",
           |    "extensionHash": "cd29c9fb3aa51cad5c63e9d72cbddc30217b7b0c10f2d2ca7c8ba181aa05320a",
           |    "powSolutions": {
           |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
           |      "w": "020a7dc1b3cc5f2f7c375e2dce16fbeab4a7b33a492f6ed425ff46e0eed6a04dee",
           |      "n": "000005816868d469",
           |      "d": 1.8575095131017126e+65
           |    },
           |    "adProofsId": "f810a115c1f41231295166784c8319aa1174b96fd920eaba958be893d99f68ff",
           |    "transactionsId": "c494f8e0e0b3dc81cd5214973acd922df167eebb45cd6501d0a5b0dce0df7bed",
           |    "parentId": "afa1b7f536f175d6a60379f8427588b75ebe47d3fd4af8c244e74e26d8f3ec5e"
           |  },
           |  {
           |    "extensionId": "5f4f565bc05c6a2eb15915c5f91e8743727e3993970f14dc034dfdff24a76222",
           |    "difficulty": "236433440768",
           |    "votes": "000000",
           |    "timestamp": 1583012350555,
           |    "size": 281,
           |    "stateRoot": "d04920b2b6d14b6fa544b7a81a47f4d54fb1e699ea34213321f86a6655b2e35414",
           |    "height": 115488,
           |    "nBits": 87493768,
           |    "version": 1,
           |    "id": "4478fa4af78dfd4f09c5cf49681b69578d28b99b538dd2657ca4ab8e5019e3d5",
           |    "adProofsRoot": "b4b18f4eb4ad5d7ab53b197b52a13ed48374c00c67f10e179d61d49ca51b12ad",
           |    "transactionsRoot": "cd40c8c17a139c9c3efaf08f54c5f23f7c0e2d3ada0e6349220be647f0be816c",
           |    "extensionHash": "cd6334b411c9385c7d548ec30623ce5651cd8a041a4f902f1957fa2e69583a3e",
           |    "powSolutions": {
           |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
           |      "w": "039f38fd49ca34f2bfa2a86bbfc59c4059d3e4b38f1b3a9c35d94f716883b8dc0e",
           |      "n": "000005cfedaf2b66",
           |      "d": 4.211745323117953e+64
           |    },
           |    "adProofsId": "ed9f57975c670d7e23a42255eccbc59f35d67360e070f3e2aadf0008ab2de47d",
           |    "transactionsId": "db21d23709c0c14660e28f0bce3abc74a21c380d49d8ee34732ae8e48d2c6366",
           |    "parentId": "c2ad321d5f7a03d478838c450a52cd352721569310b1f94339b0d88070e6f736"
           |  },
           |  {
           |    "extensionId": "7c3c6800593a6a148baa057f94a1aee553f8ac1247864191d839e526e0cfe67c",
           |    "difficulty": "236433440768",
           |    "votes": "000000",
           |    "timestamp": 1583022097453,
           |    "size": 281,
           |    "stateRoot": "3873e4e4b7478ea537b6e360811aa339039c8363c6f6bd3e6fb39a2d5ee6657714",
           |    "height": 115489,
           |    "nBits": 87493768,
           |    "version": 1,
           |    "id": "ee7eec7b2c7a239a50227699506a602ed7712c273fb2acaabe2fbab345271a65",
           |    "adProofsRoot": "7126879aa1b90d3754b1b2ced0866fcb579830081a6915aea816cae2c2623035",
           |    "transactionsRoot": "6dfe753478183ea9a3a623d0a7e3dbfb5228959b808f580ab4078be93b11ea33",
           |    "extensionHash": "f74cd224ac2d7e93ae1a2ffb459c8a8392c9c655d1ec6f2cdab739f6bf1b5808",
           |    "powSolutions": {
           |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
           |      "w": "03f0a6af4fc35fe63394f11c15f6d5681c233fb08ac7fcf17d5d6c6973aca590c2",
           |      "n": "000005efc703e726",
           |      "d": 6.604001905250926e+64
           |    },
           |    "adProofsId": "7967580811ca95e170c79fdff03aa0ac21afe1d4d627e567c5597c7460c47e83",
           |    "transactionsId": "0d33247cacd0b617a1b8f490f2bd36822daef5c0bbf48bfe7ffaae3f62b7d91b",
           |    "parentId": "4478fa4af78dfd4f09c5cf49681b69578d28b99b538dd2657ca4ab8e5019e3d5"
           |  },
           |  {
           |    "extensionId": "1a615ab137c7c6398e0836c8600d9666c6088d2f8a80aafd26d7aa84893b5af0",
           |    "difficulty": "236433440768",
           |    "votes": "000000",
           |    "timestamp": 1583026046151,
           |    "size": 282,
           |    "stateRoot": "82b20d745122bfc262ed7ad97a4f58533baae77df0ce1368ccb7860a686f141714",
           |    "height": 115490,
           |    "nBits": 87493768,
           |    "version": 1,
           |    "id": "927f462e07a9001aa77683db3a3adb2803ef4bf8c41a110c8397ad5e11306847",
           |    "adProofsRoot": "d17895a380fe21dd282d38b04196706bef8550e8525dec1e47b32c33db1234c4",
           |    "transactionsRoot": "bd0a4ee1ec89c688d1b2d8e027a2f8f5fe94e308724c464209c9a010a564fb01",
           |    "extensionHash": "1ce28f1bd809cb0f94bddd33d759e06018d1fe577ed1b228cc43ef280c93eb0d",
           |    "powSolutions": {
           |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
           |      "w": "030aab652413dc2af8830316cfa8fc680992483f9d31fc8c94c2123d9d5a93a5da",
           |      "n": "0000062d61ad0a85",
           |      "d": 1.9651612437025494e+65
           |    },
           |    "adProofsId": "3f86b5b8b3134e7be6f9ddc1d15750c96db334f4ebfa4ffe495e07462a6f8ae5",
           |    "transactionsId": "ae83c7b9a04193359900e4a2dd94d24bf154395c44468c6e5a84df04ded75302",
           |    "parentId": "ee7eec7b2c7a239a50227699506a602ed7712c273fb2acaabe2fbab345271a65"
           |  },
           |  {
           |    "extensionId": "de24c0aa5e549d22f6d0c62dda738712a3ec36e74b9bf4be6b33958e8b3090a6",
           |    "difficulty": "236433440768",
           |    "votes": "000000",
           |    "timestamp": 1583033673791,
           |    "size": 282,
           |    "stateRoot": "224e85b22bdb9147cf5b705dde4b4ce45133570e573e98588f47f54825d5d37014",
           |    "height": 115491,
           |    "nBits": 87493768,
           |    "version": 1,
           |    "id": "91aa9b160ea241c029da4c8f13d04692618eae71efff57ef59ed775977cdc996",
           |    "adProofsRoot": "dca2b87bb46f32c3a59106d9e8210b085a07b7b317d3054630afece6f0fa2c8d",
           |    "transactionsRoot": "3d16460bc854f83f41f3fb2d0863fdf63709c18faa6f71ae896620694100e5b3",
           |    "extensionHash": "b7e81ab1f63e4a180637fe537c526177f4d72416ccc802509a6b22e6584efebe",
           |    "powSolutions": {
           |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
           |      "w": "02d15bc80c30b594c14487918cd3417d192f0a7f4025292a9082bed7538e23babc",
           |      "n": "0000068787a4345d",
           |      "d": 3.18898040686667e+65
           |    },
           |    "adProofsId": "137c4b6dae297a307b0bd9ec9ac7fb8a536d5a0343e1e77b408ea11dbba920d8",
           |    "transactionsId": "df64f8e6dc6522a521ff7826cd50e0a4f254b24e70a77c68eda1197527a5b3a4",
           |    "parentId": "927f462e07a9001aa77683db3a3adb2803ef4bf8c41a110c8397ad5e11306847"
           |  },
           |  {
           |    "extensionId": "296769f18f67506d4faa674ed19a576ab52081eccebe4b62fc2dcf90d9553d70",
           |    "difficulty": "236433440768",
           |    "votes": "000000",
           |    "timestamp": 1583044834956,
           |    "size": 282,
           |    "stateRoot": "893a733498be459393b3c078fadda0cdf90d032d63eca7160e7360e9de09ae2814",
           |    "height": 115492,
           |    "nBits": 87493768,
           |    "version": 1,
           |    "id": "32166b881e193ce58f34515cefda13db19c52c7ae931163c324416911ae6063f",
           |    "adProofsRoot": "8cf62fc7feef9d7ecf46c495e1dac288d745d7f9dc6d59736f9f9da6373e9c85",
           |    "transactionsRoot": "b747590c4e168d8184345764ead8b6c4d4fb6d816396bc92c9db008bc3fdb3cd",
           |    "extensionHash": "b7e81ab1f63e4a180637fe537c526177f4d72416ccc802509a6b22e6584efebe",
           |    "powSolutions": {
           |      "pk": "0354efc32652cad6cf1231be987afa29a686af30b5735995e3ce51339c4d0ca380",
           |      "w": "02c312d49af7fe93ed23467c2d5dec85e6b19aff39e013ab83103b43da7cae7974",
           |      "n": "000006ac8ccf756b",
           |      "d": 4.1082163507546172e+65
           |    },
           |    "adProofsId": "960cd58d1610bad6a6b00eb2956886275ab087a81dcf210f49c2a39190a77d8d",
           |    "transactionsId": "4e1d785e58bfc34259da1bee7f9d4a2fab3fea190aa1b16ed8b4b110b8c83a4a",
           |    "parentId": "91aa9b160ea241c029da4c8f13d04692618eae71efff57ef59ed775977cdc996"
           |  },
           |  {
           |    "extensionId": "066ad39409d464189f595d8ebf64bbda3fae826917b24567268ecef7fd94acee",
           |    "difficulty": "236433440768",
           |    "votes": "000000",
           |    "timestamp": 1583049472755,
           |    "size": 281,
           |    "stateRoot": "e07df5338101000c8038ac92b810e6ae8bc0593b5ae08894d0b86de8d15ed48314",
           |    "height": 115493,
           |    "nBits": 87493768,
           |    "version": 1,
           |    "id": "4ebd9bc005b9e87153e8359cbc727341f85f47596d8ebb4151176f1686e0fcfb",
           |    "adProofsRoot": "9282ab52a2a86f24e307e927a590b4c339c66f7a3634322fc3aea3208f16d3dd",
           |    "transactionsRoot": "01e56689e2d7deb5bbacf2488f86cfdead138d53d5c05b72e9064f9f5a392ef3",
           |    "extensionHash": "b7e81ab1f63e4a180637fe537c526177f4d72416ccc802509a6b22e6584efebe",
           |    "powSolutions": {
           |      "pk": "0396febf0fdef6b2288b66dadf4b43d3fca96873d1b119a7319c2e0f6af2adc435",
           |      "w": "0349c0e702c529e4905bb87a818fcf4876872b8a855f56e6655439073be1fcf1e4",
           |      "n": "000000321d4f47cc",
           |      "d": 1.496906769415207e+65
           |    },
           |    "adProofsId": "0485b3d4603f50191f5135cedf640a4523f02c89fda002edad549c321244d16f",
           |    "transactionsId": "9c753593f6f249a02de4d73c74378ec1a7dd8831e477ff98b636c72074ba7f76",
           |    "parentId": "32166b881e193ce58f34515cefda13db19c52c7ae931163c324416911ae6063f"
           |  }
           |]
           |""".stripMargin)
    }
  }

}