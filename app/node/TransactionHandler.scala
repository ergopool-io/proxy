package node

import helpers.{ConfigTrait, Helper}
import loggers.Logger
import models.BoxFinder
import org.ergoplatform.appkit._
import org.ergoplatform.appkit.impl.{BlockchainContextImpl, InputBoxImpl}

import scala.collection.JavaConverters._

class TransactionHandler (client: NodeClient,
                          lockAddress: String,
                          withdrawAddress: String,
                          mnemonic: proxy.Mnemonic) extends ConfigTrait {

  private val boxConfirmation: Int = readKey("proxy.transaction.boxConfirmation", "3").toInt
  private val minerAddress: String = client.minerAddress
  updateProtectedAddress(minerAddress, lockAddress, withdrawAddress)
  private var poolTransaction: Transaction = _ // Pool transaction with custom protected input boxes
  private var customTransaction: Transaction = _
  private val ergoClient = client.ergoClient
  private var protectedAddress: String = _
  private val boxFinder = new BoxFinder()

  /**
   * returns pool transaction, which is a transaction that has custom protected boxes
   * as its inputs and boxes spendable by pool as its output
   * boxes
   *
   * @param poolAddress wallet address of the pool
   * @param value       value of the transaction without fee
   * @return pool transaction
   */
  def getPoolTransaction(poolAddress: String, value: Long): Transaction = {
    if (!isTransactionValid(poolTransaction)) {
      if (poolTransaction != null) Logger.debug(s"pool transaction is not valid, creating new one. old: ${poolTransaction.id}")
      poolTransaction = null
      createPoolTransaction(poolAddress, value)
      if (poolTransaction != null) Logger.debug(s"pool transaction has changed, new one: ${poolTransaction.id}")
    }
    poolTransaction
  }

  /**
   * returns custom transaction, which is a transaction that has regular boxes
   * as its inputs and custom protected boxes as its outputs
   *
   * @param value value of the transaction without fee
   * @return custom transaction
   */
  def getCustomTransaction(value: Long): Transaction = {
    if (!isTransactionValid(customTransaction)) {
      if (customTransaction != null) Logger.debug(s"custom transaction is not valid, creating new one. old: ${customTransaction.id}")
      customTransaction = null
      createCustomTransaction(value)
      if (customTransaction != null) Logger.debug(s"custom transaction has changed, new one: ${customTransaction.id}")
    }
    customTransaction
  }

  /**
   * checks validity of the transaction
   *
   * @param transaction to check its validity
   * @return true if transaction is valid, false otherwise
   */
  private def isTransactionValid(transaction: Transaction): Boolean = {
    if (transaction != null) {
      val inputs = transaction.getInputIds
      return boxFinder.getAvailableBoxesCount(inputs) == inputs.length
    }
    false
  }

  /**
   * creates pool transaction
   *
   * @param poolAddress address of the pool
   * @param value       value of the transaction
   */
  private def createPoolTransaction(poolAddress: String, value: Long): Unit = {
    val valueWithFee = value + client.transactionFee
    val height = Helper.ArrayByte(client.info.body).toJson.hcursor.downField("fullHeight").as[Int].getOrElse(boxConfirmation) - boxConfirmation
    val boxes = boxFinder.unspentBoxesWithTotalValue(valueWithFee, include = Vector(protectedAddress), maxHeight = height)
    if (boxes.isEmpty) {
      Logger.debug("not enough protected boxes for pool transaction@")
      return
    }

    var total = 0L
    boxes.foreach(f => total = total + f.value)
    assert(total >= valueWithFee)

    val txJson: String = this.ergoClient.execute((ctx: BlockchainContext) => {
      val ctxImpl = ctx.asInstanceOf[BlockchainContextImpl]
      val prover = ctx.newProverBuilder()
        .withMnemonic(Mnemonic.create(SecretString.create(mnemonic.value), SecretString.create("")))
        .build()
      val ph = ctx.createPreHeader()
        .height(ctx.getHeight + 1)
        .minerPk(Address.create(minerAddress).getPublicKeyGE).build()

      val txB = ctx.newTxBuilder().preHeader(ph)
      val newBox = txB.outBoxBuilder
        .value(value)
        .contract(
          ctx.compileContract(
            ConstantsBuilder.create()
              .item("poolPk", Address.create(poolAddress).getPublicKey)
              .build(),
            "{ poolPk }"))
        .build()
      val tx = txB.boxesToSpend(boxes.map(f => new InputBoxImpl(ctxImpl, f.toErgoBox).asInstanceOf[InputBox]).asJava)
        .outputs(newBox)
        .fee(client.transactionFee)
        .sendChangeTo(Address.create(withdrawAddress).getErgoAddress)
        .build()

      val signed: SignedTransaction = prover.sign(tx)

      signed.toJson(false)
    })
    poolTransaction = Transaction(txJson)
  }


  /**
   * creates custom transaction
   *
   * @param value value of the transaction considering the fee
   */
  private def createCustomTransaction(value: Long): Unit = {
    val valueWithFee = value + client.transactionFee
    val height = Helper.ArrayByte(client.info.body).toJson.hcursor.downField("fullHeight").as[Int].getOrElse(boxConfirmation) - boxConfirmation
    val inputsRaw = boxFinder.unspentBoxesWithTotalValue(valueWithFee, exclude = Vector(protectedAddress), maxHeight = height)
    if (inputsRaw.isEmpty)
      return

    var total = 0L
    inputsRaw.foreach(f => total = total + f.value)
    assert(total >= valueWithFee)

    val res = client.generateTransaction(protectedAddress, value, inputsRaw)

    if (res.isSuccess) {
      customTransaction = Transaction(res.body)
    }
  }

  /**
   * updates protectedAddress in case addresses have changed
   */
  def updateProtectedAddress(minerAddress: String, lockAddress: String, withdrawAddress: String): Unit = {
    val _protectionScript: String =
      """
        |{"source": "(proveDlog(CONTEXT.preHeader.minerPk).propBytes == PK(\"<miner>\").propBytes) && PK(\"<lock>\") || PK(\"<withdraw>\")"}
        |""".stripMargin

    val cur = _protectionScript.replaceAll("<miner>", minerAddress).replaceAll("<lock>", lockAddress).replaceAll("<withdraw>", withdrawAddress)
    val response = client.p2sAddress(cur)

    protectedAddress = Helper.ArrayByte(response.body).toJson.hcursor.downField("address").as[String].getOrElse("")
    Logger.info(s"protection script is: $protectedAddress")
  }

}
