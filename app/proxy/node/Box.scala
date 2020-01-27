package proxy.node

import helpers.Helper
import proxy.Config
import scalaj.http.Http

/**
 * Class for box of node
 * @param id [[String]] identifier of box
 * @param value [[Long]] value of box
 */
class Box (id: String, value: Long) {
  private var _bytes: String = _
  var spent: Boolean = false

  def bytes: String = {
    if (this._bytes == null) {
      val response = Http(s"${Config.nodeConnection}/utxo/byIdBinary/$id").asBytes
      if (response.isSuccess) {
        this._bytes = Helper.ArrayByte(response.body).toJson.hcursor.downField("bytes").as[String].getOrElse("")
      }
    }
    this._bytes
  }

  def boxId: String = id

  def boxValue: Long = value


}
