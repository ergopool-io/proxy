package proxy.node

import com.google.gson.Gson
import helpers.Helper
import io.circe.Json
import org.ergoplatform._
import org.ergoplatform.appkit.impl.{BlockchainContextImpl, InputBoxImpl}
import org.ergoplatform.appkit.{BlockchainContext, InputBox}
import org.ergoplatform.restapi.client.{ErgoTransactionOutput, WalletBox}
import proxy.Config
import scalaj.http.Http
import sigmastate.SSigmaProp
import sigmastate.Values.{SigmaPropConstant, Value}
import sigmastate.basics.DLogProtocol.ProveDlogProp

class ProxyBox(blockchainContext: BlockchainContextImpl, ergoBox: ErgoBox) extends InputBoxImpl(blockchainContext, ergoBox) {
  var spent: Boolean = false
  private var _bytes: String = _

  /**
   * Get binary of the box
   *
   * @return bytes
   */
  def bytes: String = {
    if (this._bytes == null) {
      val response = Http(s"${Config.nodeConnection}/utxo/byIdBinary/${this.getId}").asBytes
      if (response.isSuccess) {
        this._bytes = Helper.ArrayByte(response.body).toJson.hcursor.downField("bytes").as[String].getOrElse("")
      }
    }
    this._bytes
  }

  /**
   * Get address of the box from its ergo tree
   *
   * @return the address
   */
  def address: String = {
    val proposition = this.getErgoTree
    val ergoAddressEncoder = new ErgoAddressEncoder(Config.networkType.networkPrefix)
    val address = proposition.root match {
      case Right(SigmaPropConstant(ProveDlogProp(d))) => P2PKAddress(d)(ergoAddressEncoder)
      case Right(ergoAddressEncoder.IsPay2SHAddress(scriptHash)) => new Pay2SHAddress(scriptHash.toArray)(ergoAddressEncoder)
      case Right(b: Value[SSigmaProp.type]@unchecked) if b.tpe == SSigmaProp => Pay2SAddress(proposition)(ergoAddressEncoder)
      case _ => null
    }
    address.toString
  }

  override def toString: String = {
    s"($getValue, $spent, $getId)"
  }
}

object ProxyBox {
  def apply(ctx: BlockchainContext, inputBox: InputBox): ProxyBox = {
    new ProxyBox(ctx.asInstanceOf[BlockchainContextImpl], inputBox.asInstanceOf[InputBoxImpl].getErgoBox)
  }

  def apply(ctx: BlockchainContext, ergoBox: ErgoBox): ProxyBox = {
    new ProxyBox(ctx.asInstanceOf[BlockchainContextImpl], ergoBox)
  }

  def apply(ctx: BlockchainContext, json: Json): ProxyBox = {
    val gson = new Gson()
    val walletBox = gson.fromJson(json.toString(), classOf[WalletBox])
    ProxyBox(ctx, walletBox.getBox)
  }

  def apply(ctx: BlockchainContext, ergoTransactionOutput: ErgoTransactionOutput): ProxyBox = {
    new ProxyBox(ctx.asInstanceOf[BlockchainContextImpl], new InputBoxImpl(ctx.asInstanceOf[BlockchainContextImpl], ergoTransactionOutput).getErgoBox)
  }
}
