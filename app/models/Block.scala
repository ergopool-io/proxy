package models

import io.ebean.Model
import javax.persistence.{CascadeType, Column, Entity, Id, OneToMany}

@Entity
class Block extends Model {
  @Id
  var id: Int = _

  @Column(unique = true, nullable = false)
  var blockId: String = _

  @Column(nullable = false)
  var height: Int = _

  // $COVERAGE-OFF$
  override def toString: String = {
    s"""
      |{
      |   "id": $id,
      |   "blockId": "$blockId",
      |   "height": $height,
      |}
      |""".stripMargin
  }
  // $COVERAGE-ON$
}

object Block {
  def apply(blockId: String, height: Int): Block = {
    val block = new Block()
    block.blockId = blockId
    block.height = height
    block
  }
}