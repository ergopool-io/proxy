package models

import com.google.gson.Gson
import helpers.Helper
import io.circe.Json
import io.ebean.Model
import javax.persistence._
import org.ergoplatform.appkit.impl.ScalaBridge
import org.ergoplatform.restapi.client.ErgoTransactionOutput
import org.ergoplatform._
import loggers.Logger
import node.NodeClient
import org.ergoplatform.appkit.NetworkType
import scorex.util.ModifierId
import sigmastate.SSigmaProp
import sigmastate.Values.{ErgoTree, SigmaPropConstant, Value}
import sigmastate.basics.DLogProtocol.ProveDlogProp
import io.ebean.annotation.{ConstraintMode, DbForeignKey}

@Entity
class Box extends Model {
  @Id
  var id: String = _

  @Column(nullable = false)
  var value: Long = _

  @Column(nullable = false)
  var creationHeight: Int = _

  @Column(nullable = false)
  var boxIndex: Int = _

  @Column(nullable = false)
  var address: String = _

  @Column(nullable = false, length = 500)
  var ergoTree: String = _

  @Column(length = 100)
  var transactionId: String = _

  @Column(length = 500)
  var bytes: String = _

  @Column(nullable = false)
  var inclusionHeight: Int = _

  @ManyToOne
  @DbForeignKey(onDelete = ConstraintMode.SET_NULL)
  var spentIn: Block = _

  @ManyToOne(optional = false)
  @DbForeignKey(onDelete = ConstraintMode.CASCADE)
  var createdIn: Block = _

  // $COVERAGE-OFF$
  override def toString: String = {
    s"""
       |{
       |   "id": "$id",
       |   "value": $value,
       |   "ergoTree": "$ergoTree",
       |   "transactionId": "$transactionId",
       |   "index": $boxIndex,
       |   "creationHeight": $creationHeight,
       |   "spentIn": $spentIn,
       |   "createdIn": $createdIn,
       |   "address": "$address"
       |}
       |""".stripMargin
  }
  // $COVERAGE-ON$

  /**
   * Get binary of the box
   *
   * @return bytes
   */
  def boxBytes(client: NodeClient): String = {
    if (this.bytes == null) {
      val response = client.getBoxBytes(id)
      if (response.isSuccess) {
        this.bytes = Helper.ArrayByte(response.body).toJson.hcursor.downField("bytes").as[String].getOrElse("")
      }
    }
    this.bytes
  }

  /**
   * Convert the Box to an ErgoBox
   *
   * @return an ergo box type of the box
   */
  def toErgoBox: ErgoBox = {
    ErgoBox(value, ScalaBridge.isoStringToErgoTree.to(ergoTree),
      creationHeight, Seq(), Map(), ModifierId @@ transactionId)
  }

  def handleOwnerBlocks(): Unit = {
    if (this.spentIn != null && this.spentIn.id == 0) {
      val block = Box.blockFinder.byBlockId(this.spentIn.blockId)
      if (block != null) {
        this.spentIn = block
      } else {
        this.spentIn.save()
      }
    }
    if (this.createdIn != null && this.createdIn.id == 0) {
      val block = Box.blockFinder.byBlockId(this.createdIn.blockId)
      if (block != null) {
        this.createdIn = block
      } else {
        this.createdIn.save()
      }
    }
  }

  override def update(): Unit = {
    handleOwnerBlocks()
    super.update()
  }

  override def save(): Unit = {
    if (new BoxFinder().query().where().idEq(this.id).exists()) {
      this.update()
      return
    }
    handleOwnerBlocks()
    super.save()

  }
}

object Box {
  val blockFinder = new BlockFinder
  def apply(boxJson: Json, networkType: NetworkType, inclusionHeight: Int): Box = {
    val gson = new Gson()
    val transactionOutput = gson.fromJson(boxJson.toString(), classOf[ErgoTransactionOutput])

    apply(transactionOutput, ergoTree2Address(ScalaBridge.isoStringToErgoTree.to(transactionOutput.getErgoTree), networkType), inclusionHeight)
  }

  def apply(boxJson: Json, address: String, inclusionHeight: Int): Box = {
    val gson = new Gson()
    val transactionOutput = gson.fromJson(boxJson.toString(), classOf[ErgoTransactionOutput])

    apply(transactionOutput, address, inclusionHeight)
  }

  def apply(transactionOutput: ErgoTransactionOutput, address: String, inclusionHeight: Int): Box = {
    val box = new Box()
    box.id = transactionOutput.getBoxId
    box.transactionId = transactionOutput.getTransactionId
    box.address = address
    box.ergoTree = transactionOutput.getErgoTree
    box.value = transactionOutput.getValue
    box.creationHeight = transactionOutput.getCreationHeight
    box.boxIndex = transactionOutput.getIndex
    box.inclusionHeight = inclusionHeight
    box
  }

  /**
   * Get address of the box from its ergo tree
   *
   * @return the address
   */
  private def ergoTree2Address(ergoTree: ErgoTree, networkType: NetworkType): String = {
    val proposition = ergoTree
    val ergoAddressEncoder = new ErgoAddressEncoder(networkType.networkPrefix)
    val address = proposition.root match {
      case Right(SigmaPropConstant(ProveDlogProp(d))) => P2PKAddress(d)(ergoAddressEncoder)
      case Right(ergoAddressEncoder.IsPay2SHAddress(scriptHash)) => new Pay2SHAddress(scriptHash.toArray)(ergoAddressEncoder)
      case Right(b: Value[SSigmaProp.type]@unchecked) if b.tpe == SSigmaProp => Pay2SAddress(proposition)(ergoAddressEncoder)
      case _ => null
    }
    address.toString
  }
}