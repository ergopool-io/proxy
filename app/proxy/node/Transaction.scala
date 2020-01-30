package proxy.node

import helpers.Helper
import io.circe.Json
import proxy.Config
import scalaj.http.Http

/**
 * Transaction of node
 * @param id [[String]] identifier of transaction
 * @param details [[Json]] whole body of transaction
 */
case class Transaction(id: String, details: Json) {
  def isMined: Boolean = {
    val response = Http(s"${Config.nodeConnection}/wallet/transactionById?id=$id").header("api_key", Config.apiKey).asBytes

    if (response.isSuccess)
      true
    else
      false
  }
}

object Transaction {
  def apply(responseBody: Array[Byte]): Transaction = {
    val jsonBody = Helper.ArrayByte(responseBody).toJson
    val txsId = jsonBody.hcursor.downField("id").as[String].getOrElse("")
    new Transaction(txsId, jsonBody)
  }
}
