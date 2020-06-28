package node

import helpers.Helper
import io.circe.Json

/**
 * Transaction of node
 * @param id [[String]] identifier of transaction
 * @param details [[Json]] whole body of transaction
 */
case class Transaction(id: String, details: Json) {
  /**
   * Get ids of the input boxes of this transaction
   *
   * @return list of input boxes' ids
   */
  def getInputIds: Vector[String] = {
    var inputs = Vector[String]()
    details.hcursor.downField("inputs").as[Vector[Json]].getOrElse(Vector()).foreach(input => {
      val id = input.hcursor.downField("boxId").as[String].getOrElse(null)
      if (id != null) {
        inputs = inputs :+ id
      }
    })
    inputs
  }

  @Override
  override def equals(obj: Any): Boolean = {
    obj match {
      case transaction: Transaction =>
        id == transaction.id && details == transaction.details

      case _ =>
        super.equals(obj)
    }
  }
}

object Transaction {
  def apply(responseBody: Array[Byte]): Transaction = {
    val jsonBody = Helper.ArrayByte(responseBody).toJson
    val txsId = jsonBody.hcursor.downField("id").as[String].getOrElse("")
    new Transaction(txsId, jsonBody)
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
