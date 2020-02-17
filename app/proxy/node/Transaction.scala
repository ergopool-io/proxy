package proxy.node

import helpers.Helper
import io.circe.Json
import org.ergoplatform.appkit._
import proxy.Config
import scalaj.http.Http

/**
 * Transaction of node
 *
 * @param id      [[String]] identifier of transaction
 * @param details [[Json]] whole body of transaction
 */
case class Transaction(id: String, details: Json) {
  /**
   * Check if the transaction is mined
   *
   * @return
   */
  def isMined: Boolean = {
    val response = Http(s"${Config.nodeConnection}/wallet/transactionById?id=$id")
      .headers(("api_key", Config.apiKey), ("Content-Type", "application/json")).asBytes

    if (response.isSuccess)
      true
    else
      false
  }

  /**
   * Get output of the transaction, with an specific address if defined
   *
   * @param address the address of boxes
   * @return
   */
  def outputBoxes(address: String = null): Vector[ProxyBox] = {
    val ergoClient = RestApiErgoClient.create(Config.nodeConnection, Config.networkType, Config.apiKey)

    ergoClient.execute((ctx: BlockchainContext) => {
      val tx = ctx.signedTxFromJson(this.details.toString())
      var jsonBoxes = Vector[ProxyBox]()
      tx.getOutputsToSpend.forEach(f => {
        val val0 = ProxyBox(ctx, f)
        if (address == null || address == val0.address)
          jsonBoxes = jsonBoxes :+ val0
      })
      jsonBoxes
    })
  }

  def inputBoxes: Vector[SignedInput] = {
    val ergoClient = RestApiErgoClient.create(Config.nodeConnection, Config.networkType, Config.apiKey)

    ergoClient.execute((ctx: BlockchainContext) => {
      val tx = ctx.signedTxFromJson(this.details.toString())
      var boxes = Vector[SignedInput]()
      tx.getSignedInputs.forEach(f => {
        boxes = boxes :+ f
      })
      boxes
    })
  }
}

object Transaction {
  def apply(responseBody: Array[Byte]): Transaction = {
    val jsonBody = Helper.ArrayByte(responseBody).toJson
    Transaction(jsonBody)
  }

  def apply(stringBody: String): Transaction = {
    val jsonBody = Helper.convertToJson(stringBody)
    Transaction(jsonBody)
  }

  def apply(jsonBody: Json): Transaction = {
    val txsId = jsonBody.hcursor.downField("id").as[String].getOrElse("")
    new Transaction(txsId, jsonBody)
  }
}
