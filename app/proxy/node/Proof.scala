package proxy.node

import helpers.Helper
import io.circe.{ACursor, Json}

case class Proof(body: Json)

object Proof {
  private def createProof(body: Json): Proof = {
    if (body.isNull) return null
    val cursor: ACursor = body.hcursor
    val txProof: ACursor = cursor.downField("txProofs").downArray

    val proof =
      s"""
         |{
         |    "pk": "${Node.pk}",
         |    "msg_pre_image": "${cursor.downField("msgPreimage").as[String].getOrElse("")}",
         |    "leaf": "${txProof.downField("leaf").as[String].getOrElse("")}",
         |    "levels": ${txProof.downField("levels").as[Json].getOrElse(Json.Null)}
         |}
         |""".stripMargin
    new Proof(Helper.convertToJson(proof))
  }

  def apply(body: Json): Proof = createProof(body)
}
