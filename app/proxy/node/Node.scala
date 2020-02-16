package proxy.node

import com.google.gson.Gson
import helpers.Helper
import io.circe.{HCursor, Json}
import org.ergoplatform.appkit._
import org.ergoplatform.appkit.impl.BlockchainContextImpl
import org.ergoplatform.restapi.client.{NodeInfo, WalletBox}
import play.api.mvc.{RawBuffer, Request}
import proxy.loggers.{DebugLogger, Logger}
import proxy.status.ProxyStatus.{MiningDisabledException, NotEnoughBoxesException}
import proxy.status.{ProxyStatus, StatusType}
import proxy.{Config, PoolShareQueue, Response}
import scalaj.http.{Http, HttpResponse}

import scala.collection.JavaConverters._


object Node {
  val heightBatchSize: Int = 1000
  private val _protectionScript: String =
    """
      |{"source": "(proveDlog(CONTEXT.preHeader.minerPk).propBytes == PK(\"<miner>\").propBytes) && PK(\"<lock>\") || PK(\"<withdraw>\")"}
      |""".stripMargin
  var pk: String = ""
  var protectedUnspentBoxes: Vector[ProxyBox] = Vector[ProxyBox]()
  var unprotectedUnspentBoxes: Vector[ProxyBox] = Vector[ProxyBox]()
  var inclusionHeight: Int = _
  private var _lastProtectionAddress: String = _
  private var remainBoxesTransaction: Transaction = _
  private var txsList: Vector[Transaction] = Vector[Transaction]()
  private var poolTransaction: Transaction = _
  private var _gapTransaction: Transaction = _

  /**
   * Getter for gap transaction
   *
   * @return the gap transaction
   */
  def gapTransaction: Transaction = _gapTransaction

  /**
   * Get ergo clients for parts that needs blockchainContext
   *
   * @return ergo client
   */
  def ergoClient: ErgoClient = {
    RestApiErgoClient.create(Config.nodeConnection, Config.networkType, Config.apiKey)
  }

  /**
   * Get id of last pool transaction
   *
   * @return id of last pool transaction
   */
  def lastPoolTransactionId: String = if (this.poolTransaction != null) this.poolTransaction.id else null

  /**
   * Call derive key endpoint of node to get address from key
   *
   * @param key [[String]] the key derive address from
   * @return derived address
   */
  def deriveKey(key: String): String = {
    val response = Http(s"${Config.nodeConnection}/wallet/deriveKey")
      .postData(s"""{"derivationPath": "$key"}""")
      .headers(Seq[(String, String)](("api_key", Config.apiKey), ("Content-Type", "application/json")))
      .asBytes

    Helper.ArrayByte(response.body).toJson.hcursor.downField("address").as[String].getOrElse("")
  }

  /**
   * Get addresses of wallet from the node
   *
   * @return vector of addresses
   */
  def walletAddresses: Vector[String] = {
    val response = Http(s"${Config.nodeConnection}/wallet/addresses").headers(("api_key", Config.apiKey), ("Content-Type", "application/json")).asBytes
    Helper.ArrayByte(response.body).toJson.asArray.getOrElse(Vector[Json]()).map(f => f.as[String].getOrElse(""))
  }

  /**
   * Clear txs list
   */
  def resetTxsList(): Unit = {
    this.txsList = Vector[Transaction]()
  }

  /**
   * Send a request to a url with its all headers and body
   *
   * @param uri     [[String]] Servers url
   * @param request [[Request[RawBuffer]]] The request to send
   * @return [[Response]] Response from the server
   */
  def sendRequest(uri: String, request: Request[RawBuffer]): Response = {
    // Prepare the request headers
    val reqHeaders: Seq[(String, String)] = request.headers.headers

    val response: HttpResponse[Array[Byte]] = {
      try {
        if (request.method == "GET") {
          Http(s"${Config.nodeConnection}$uri").headers(reqHeaders).asBytes
        }
        else {
          Http(s"${Config.nodeConnection}$uri").headers(reqHeaders).postData(Helper.RawBufferValue(request.body).toString).asBytes
        }
      }
      catch {
        case error: Throwable =>
          throw new Throwable(s"Node - $uri: ${error.toString}", error)
      }
    }

    // Convert the headers to Map[String, String] type
    val respHeaders: Map[String, String] = response.headers.map({
      case (key, value) =>
        key -> value.mkString(" ")
    })

    // Remove the ignored headers
    val contentType: String = respHeaders.getOrElse("Content-Type", "")

    // Return the response
    Response(
      statusCode = response.code,
      headers = reqHeaders.toMap,
      body = response.body,
      contentType = contentType
    )
  }

  /**
   * Send solution to the node
   *
   * @param request [[Request]] the request from the miner
   * @return [[Response]]
   */
  def sendSolution(request: Request[RawBuffer]): Response = {
    // Prepare the request headers
    val reqHeaders: Seq[(String, String)] = request.headers.headers
    val reqBody: HCursor = Helper.RawBufferValue(request.body).toJson.hcursor
    val body: String =
      s"""
         |{
         |  "pk": "${reqBody.downField("pk").as[String].getOrElse("")}",
         |  "w": "${reqBody.downField("w").as[String].getOrElse("")}",
         |  "n": "${reqBody.downField("n").as[String].getOrElse("")}",
         |  "d": ${reqBody.downField("d").as[BigInt].getOrElse("")}e0
         |}
         |""".stripMargin

    val rawResponse: HttpResponse[Array[Byte]] = Http(s"${Config.nodeConnection}${request.uri}").headers(reqHeaders).postData(body).asBytes
    Response(rawResponse)
  }

  /**
   * Send generate transaction request to the node
   *
   * @param address   destination address for transaction
   * @param value     value of transaction
   * @param inputsRaw input boxes for transaction
   * @return response from the node
   */
  def generateTransaction(address: String = Config.walletAddress,
                          value: Long = this.configuredTransactionTotalValue,
                          inputsRaw: Vector[ProxyBox] = null):
  HttpResponse[Array[Byte]] = {
    if (inputsRaw != null) {
      inputsRaw.foreach(box => box.spent = true)
    }
    val reqHeaders: Seq[(String, String)] = Seq(("api_key", Config.apiKey), ("Content-Type", "application/json"))
    val transactionGenerateBody: String =
      s"""
         |{
         |  "requests": [
         |    {
         |      "address": "$address",
         |      "value": $value
         |    }
         |  ],
         |  "fee": ${Config.transactionFee},
         |  "inputsRaw": [${if (inputsRaw != null) inputsRaw.map(f => s""""${f.bytes}"""").mkString(",") else ""}]
         |}
         |""".stripMargin
    Http(s"${Config.nodeConnection}/wallet/transaction/generate").headers(reqHeaders).postData(transactionGenerateBody).asBytes
  }

  /**
   * Send candidateWithTxs request to the node
   *
   * @return [[HttpResponse]]
   */
  def candidateWithTxs(): HttpResponse[Array[Byte]] = {
    val reqHeaders: Seq[(String, String)] = Seq(("api_key", Config.apiKey), ("Content-Type", "application/json"))
    DebugLogger.debug(
      s"""
        |List of transactions for candidateWithTxs:
        |${this.txsList}
        |""".stripMargin)
    val candidateWithTxsBody: String =
      s"""
         |[
         |  ${this.txsList.map(tx => s"""{"transaction": ${tx.details}, "cost": 50000}""").mkString(",")}
         |]
         |""".stripMargin
    val response = Http(s"${Config.nodeConnection}/mining/candidateWithTxs").headers(reqHeaders).postData(candidateWithTxsBody).asBytes
    this.pk = Helper.ArrayByte(response.body).toJson.hcursor.downField("pk").as[String].getOrElse("")

    response
  }

  /**
   * Parse error responses from the node and get message of error
   *
   * @param response the node response
   * @return message of error
   */
  def parseErrorResponse(response: HttpResponse[Array[Byte]]): String = {
    val body = Helper.ArrayByte(response.body).toJson
    val detail = body.hcursor.downField("detail").as[String].getOrElse("")

    val pattern = "\\([^()]*\\)".r
    var message = detail
    var newMessage = message
    while (message != newMessage) {
      newMessage = message
      message = pattern.replaceAllIn(newMessage, "")
    }
    message
  }

  /**
   * Fetch all unspent boxes from the node, add them to lists and set maximum inclusion height
   */
  def fetchUnspentBoxes(): Unit = {
    val response = Http(s"${Config.nodeConnection}/wallet/boxes/unspent")
      .headers(("api_key", Config.apiKey), ("Content-Type", "application/json")).asBytes

    if (response.isSuccess) {
      DebugLogger.debug(Helper.ArrayByte(response.body).toString)
      Helper.ArrayByte(response.body).toJson.asArray.getOrElse(Vector()).foreach(box => {
        val gson = new Gson()
        val walletBox = gson.fromJson(box.toString(), classOf[WalletBox])
        this.addWalletBox(walletBox)
        this.inclusionHeight = math.max(walletBox.getInclusionHeight, this.inclusionHeight)
      })
    }
    else {
      DebugLogger.debug(
        s"""
          |Error in fetching unspent boxes:
          |${Helper.ArrayByte(response.body).toString}
          |""".stripMargin)
    }
  }

  // $COVERAGE-OFF$

  /**
   * Add box to its related list, protected or unprotected
   *
   * @param walletBox the box to add to the lists
   */
  def addWalletBox(walletBox: WalletBox): Unit = {
    ergoClient.execute((ctx: BlockchainContext) => {
      val proxyBox = ProxyBox(ctx, walletBox.getBox)
      if (walletBox.getAddress == this.lastProtectionAddress)
        this.protectedUnspentBoxes = this.protectedUnspentBoxes :+ proxyBox
      else {
        this.unprotectedUnspentBoxes = this.unprotectedUnspentBoxes :+ proxyBox
      }
    })
  }

  // $COVERAGE-ON$

  /**
   * Check remain boxes transaction, remove spent boxes, add it to the transaction list, and
   * add its boxes to the lists if it had been mined
   */
  def checkRemainBoxesTransaction(): Unit = {
    if (this.remainBoxesTransaction != null && this.remainBoxesTransaction.isMined) {
      this.removeUnprotectedSpentBoxes()
      this.addTransaction(this.remainBoxesTransaction)
      this.addBoxes(this.remainBoxesTransaction)
      this.remainBoxesTransaction = null
    }
  }

  /**
   * Add the transaction to the transactions list
   *
   * @param transaction [[Transaction]] the transaction to add
   */
  def addTransaction(transaction: Transaction): Unit = {
    this.txsList = this.txsList :+ transaction
  }

  /**
   * Add boxes of transaction to the protected boxes lists which has the protected address
   *
   * @param transaction the transaction containing boxes
   */
  def addBoxes(transaction: Transaction): Unit = {
    protectedUnspentBoxes = protectedUnspentBoxes ++ transaction.boxes(this.lastProtectionAddress)
  }

  /**
   * Get last created protection address from the protection script
   *
   * @return protection script address
   */
  def lastProtectionAddress: String = {
    if (_lastProtectionAddress == null)
      this.createProtectionScript()
    _lastProtectionAddress
  }

  /**
   * Create protection script address
   */
  def createProtectionScript(): Unit = {
    val response = Http(s"${Config.nodeConnection}/script/p2sAddress")
      .headers(("api_key", Config.apiKey), ("Content-Type", "application/json"))
      .postData(this.protectionScript)
      .asBytes

    require(response.isSuccess, "Could not create protection address")
    _lastProtectionAddress = Helper.ArrayByte(response.body).toJson.hcursor.downField("address").as[String].getOrElse("")
  }

  /**
   * Get protection script with its value replaced from the config file
   *
   * @return protection script
   */
  private def protectionScript: String = {
    if (pk != "")
      this._protectionScript.replaceAll("<miner>", Config.minerAddress).replaceAll("<lock>", Config.lockAddress).replaceAll("<withdraw>", Config.withdrawAddress)
    else
      ""
  }

  /**
   * Remove spent boxes from unprotected boxes list
   */
  def removeUnprotectedSpentBoxes(): Unit = {
    this.unprotectedUnspentBoxes = this.unprotectedUnspentBoxes.filter(box => !box.spent)
  }

  /**
   * Check remain boxes transaction, remove spent boxes, and
   * add its boxes to the lists if it had been mined
   */
  def checkPoolTransaction(): Unit = {
    if (this.poolTransaction != null && this.poolTransaction.isMined) {
      this.removeProtectedSpentBoxes()
      this.addBoxes(this.poolTransaction)
      this.poolTransaction = null
    }
  }

  /**
   * Remove spent boxes from protected boxes list
   */
  def removeProtectedSpentBoxes(): Unit = {
    this.protectedUnspentBoxes = this.protectedUnspentBoxes.filter(box => !box.spent)
  }

  /**
   * Get total value of unspent boxes
   *
   * @return [[Long]]
   */
  def unspentBoxesTotalValue(unspentBoxes: Vector[ProxyBox]): Long = {
    var total = 0L
    unspentBoxes.filter(p => !p.spent).foreach(f => total = total + f.getValue)
    total
  }

  /**
   * Get total value of all boxes
   *
   * @return [[Long]]
   */
  def boxesTotalValue(boxes: Vector[ProxyBox]): Long = {
    var total = 0L
    boxes.foreach(f => total = total + f.getValue)
    total
  }

  /**
   * Create the pool transaction using appkit library with specified value and using passed boxes
   *
   * @param totalToSpend value of transaction
   * @param boxes        boxes to spend for transaction
   * @return created transaction
   */
  def poolTransaction(totalToSpend: Long, boxes: Vector[ProxyBox]): Transaction = {
    require(boxes.nonEmpty, s"Not enough boxes in the wallet to pay $totalToSpend")
    val ergoClient = RestApiErgoClient.create(Config.nodeConnection, Config.networkType, Config.apiKey)

    val txJson = ergoClient.execute((ctx: BlockchainContext) => {
      val prover = ctx.newProverBuilder()
        .withMnemonic(Mnemonic.create(SecretString.create(proxy.Mnemonic.value), SecretString.create("")))
        .build()
      val ph = ctx.createPreHeader()
        .height(ctx.getHeight + 1)
        .minerPk(Address.create(Config.minerAddress).getPublicKeyGE).build()

      val txB = ctx.newTxBuilder().preHeader(ph)
      val newBox = txB.outBoxBuilder
        .value(totalToSpend)
        .contract(
          ctx.compileContract(
            ConstantsBuilder.create()
              .item("poolPk", Address.create(Config.walletAddress).getPublicKey)
              .build(),
            "{ poolPk }"))
        .build()
      val tx = txB.boxesToSpend(boxes.map(_.asInstanceOf[InputBox]).asJava)
        .outputs(newBox)
        .fee(Parameters.MinFee)
        .sendChangeTo(prover.getP2PKAddress)
        .build()

      val signed: SignedTransaction = prover.sign(tx)

      signed.toJson(false)
    })
    Transaction(txJson.toString)
  }

  /**
   * Generate a transaction with unspent boxes
   * Generates a transaction, named gap transaction, if there is not enough protected boxes to create the transaction
   *
   * @throws MiningDisabledException if there's not enough boxes
   * @return created transaction
   */
  def generateTransactionWithProtectedUnspentBoxes(): Transaction = {
    protectedUnspentBoxes.foreach(box => box.spent = false)
    val boxesValueSum = this.boxesTotalValue(this.protectedUnspentBoxes)

    if (boxesValueSum < this.configuredTransactionTotalValue) {
      this.unprotectedUnspentBoxes.foreach(box => box.spent = false)
      val neededBoxes = this.getBoxes(
        this.unprotectedUnspentBoxes,
        this.configuredTransactionTotalValue
      )
      if (this.boxesTotalValue(neededBoxes) < this.configuredTransactionTotalValue) {
        throw new MiningDisabledException("Not enough boxes")
      }


      this._gapTransaction = this.generateProtectedTransaction(this.configuredTransactionTotalValue - boxesValueSum)

      this.sendTransaction(this._gapTransaction)

      Logger.logger.info(_gapTransaction.id)
      throw new MiningDisabledException("Should wait until transaction being mined", "TxsGen")
    }

    val boxesNeededForTransaction = this.getBoxes(
      this.protectedUnspentBoxes,
      this.configuredTransactionTotalValue,
      markSpent = true
    )

    this.poolTransaction(Config.transactionRequestsValue, boxesNeededForTransaction)
  }

  /**
   * Generate a transaction for the remain boxes if needed
   */
  def handleRemainUnspentBoxes(): Unit = {
    val boxesValueSum = this.unspentBoxesTotalValue(this.protectedUnspentBoxes)
    if (boxesValueSum < this.configuredTransactionTotalValue) {
      try {
        this.remainBoxesTransaction = this.generateProtectedTransaction(this.configuredTransactionTotalValue - boxesValueSum)
        this.addTransaction(this.remainBoxesTransaction)
      } catch {
        case e: Throwable =>
          ProxyStatus.setStatus(StatusType.yellow, "Transaction Generate", e.toString)
      }
    }
  }

  /**
   * Generate transaction and make a new proof
   *
   * @return [[Proof]] The body with proof
   */
  def createProof(): Response = {
    try {
      PoolShareQueue.lock()
      this.poolTransaction = this.generateTransactionWithProtectedUnspentBoxes()

      this.addTransaction(this.poolTransaction)

      this.handleRemainUnspentBoxes()

      val candidateWithTxsResponse = this.candidateWithTxs()
      this.resetTxsList()

      DebugLogger.debug(
        s"""
          |CandidateWithTxs response:
          |${Helper.ArrayByte(candidateWithTxsResponse.body).toString}
          |""".stripMargin)

      if (candidateWithTxsResponse.isSuccess) {
        val proof = Proof(Helper.ArrayByte(candidateWithTxsResponse.body).toJson.hcursor.downField("proof").as[Json].getOrElse(Json.Null), this.lastPoolTransactionId)
        PoolShareQueue.push(this.poolTransaction, proof)
        Response(candidateWithTxsResponse)
      }
      else {
        Logger.error(s"candidateWithTxs failed: ${this.parseErrorResponse(candidateWithTxsResponse)}")
        null
      }
    }
    catch {
      case error: ProxyStatus.MiningDisabledException =>
        throw error
      case error: Throwable =>
        ProxyStatus.disableMining(s"Creating proof failed: ${error.toString}")
        throw error
    }
    finally {
      PoolShareQueue.unlock()
    }
  }

  /**
   * Sum of transaction fee and transaction requests value
   *
   * @return [[Long]]
   */
  private def configuredTransactionTotalValue: Long = Config.transactionRequestsValue + Config.transactionFee

  /**
   * Check whether throw a NotEnoughBoxesException or a MiningDisableException from the response detail
   *
   * @param response [[HttpResponse]] the response to check
   * @return [[Throwable]]
   */
  private def notEnoughBoxesOrMiningDisable(response: HttpResponse[Array[Byte]]): Throwable = {
    val errorMsg = parseErrorResponse(response)
    if (errorMsg.map(_.toLower).contains("not enough boxes"))
      new NotEnoughBoxesException("Not enough boxes on remain boxes transaction")
    else
      new MiningDisabledException(s"Transaction for remain boxes failed: $errorMsg")
  }

  /**
   * Send the transaction to the network
   *
   * @param transaction the transaction to be sent
   */
  private def sendTransaction(transaction: Transaction): Unit = {
    this.ergoClient.execute(ctx => {
      val tx = ctx.signedTxFromJson(transaction.details.toString())
      Logger.logger.info(ctx.sendTransaction(tx))
    })
  }

  /**
   * Get info of node
   *
   * @return node's info
   */
  private def getInfo: NodeInfo = {
    ergoClient.execute((ctx: BlockchainContext) => {
      ctx.asInstanceOf[BlockchainContextImpl].getNodeInfo
    })
  }

  /**
   * Get next unspent boxes starting from passed header height
   *
   * @param headerHeight starting height
   * @return list of wallet boxes of this batch
   */
  private def getNextUnspentBoxesBatch(headerHeight: Int): Vector[WalletBox] = {
    val response = Http(s"${Config.nodeConnection}/wallet/boxes/unspent?minConfirmations=${headerHeight - this.inclusionHeight - this.heightBatchSize}&minInclusionHeight=${this.inclusionHeight}")
      .headers(("api_key", Config.apiKey), ("Content-Type", "application/json")).asBytes
    Helper.ArrayByte(response.body).toJson.asArray.getOrElse(Vector[WalletBox]()).map(box => {
      val gson = new Gson()
      gson.fromJson(box.toString, classOf[WalletBox])
    })
  }

  /**
   * Get boxes that sum of their values reaches specified amount from passed list
   * Mark them as spent is true
   *
   * @param boxes     list of all boxes
   * @param amount    minimum needed amount
   * @param markSpent mark boxes as spent if true
   * @return list of boxes with total value bigger than amount
   */
  private def getBoxes(boxes: Vector[ProxyBox], amount: Long, markSpent: Boolean = false): Vector[ProxyBox] = {
    var total = 0L
    var response = Vector[ProxyBox]()
    boxes.iterator.takeWhile(_ => total <= this.configuredTransactionTotalValue).foreach(box => {
      total = total + box.getValue
      response = response :+ box
      if (markSpent) box.spent = true
    })
    response
  }

  /**
   * Get boxes from the node to fill the unprotected boxes list in order that its boxes total value reaches an amount
   * bigger than the value
   *
   * @param value        needed value
   * @param currentValue current value of the list total value
   */
  private def fillUnspentBoxes(value: Long, currentValue: Long): Unit = {
    var boxesValueSum = currentValue
    val height = this.getInfo.getFullHeight
    while (boxesValueSum < value) {
      if (height > this.inclusionHeight) {
        val boxes = getNextUnspentBoxesBatch(height)
        boxes.foreach(box => {
          boxesValueSum = boxesValueSum + box.getBox.getValue
          this.addWalletBox(box)
        })
        this.inclusionHeight = this.inclusionHeight + this.heightBatchSize
      }
      else {
        this.fetchUnspentBoxes()
        boxesValueSum = this.unspentBoxesTotalValue(this.unprotectedUnspentBoxes)
        if (boxesValueSum < value)
          throw new MiningDisabledException("Not enough boxes!")
      }
    }
  }

  /**
   * Generate a transaction with protected address
   * Tries to fill the lists if boxes value is not enough
   *
   * @param value the value of transaction
   * @return transaction with protected address
   */
  private def generateProtectedTransaction(value: Long): Transaction = {
    val boxesValueSum = this.unspentBoxesTotalValue(this.unprotectedUnspentBoxes)

    if (value > boxesValueSum)
      this.fillUnspentBoxes(value, boxesValueSum)

    val neededBoxes = this.getBoxes(
      this.unprotectedUnspentBoxes.filter(box => !box.spent),
      value,
      markSpent = true
    ) // mark spent to false if not mined

    val response = this.generateTransaction(address = this.lastProtectionAddress, value, neededBoxes)
    if (!response.isSuccess) {
      throw notEnoughBoxesOrMiningDisable(response)
    }

    Transaction(response.body)
  }
}
