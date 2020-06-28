package node

import helpers.Helper
import io.circe.{ACursor, Json}

case class Proof(body: Json)

object Proof {
  def apply(body: Json, leaf: String): Proof = createProof(body, leaf)

  private def createProof(body: Json, leaf: String): Proof = {
    if (body.isNull) return null
    val cursor: ACursor = body.hcursor
    val txProof: Vector[Json] = cursor.downField("txProofs").as[Json].getOrElse(Json.Null).asArray.get
    val proofBodies = txProof.filter(proof =>
      proof.hcursor.downField("leaf").as[String].getOrElse("") == leaf
    )
    if (proofBodies.isEmpty)
      return null

    val proofBody = proofBodies.apply(0).hcursor

    val proof =
      s"""
         |{
         |    "msg_pre_image": "${cursor.downField("msgPreimage").as[String].getOrElse("")}",
         |    "leaf": "${proofBody.downField("leaf").as[String].getOrElse("")}",
         |    "levels": ${proofBody.downField("levels").as[Json].getOrElse(Json.Null)}
         |}
         |""".stripMargin
    new Proof(Helper.convertToJson(proof))
  }
}
