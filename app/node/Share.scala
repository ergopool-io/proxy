package node

import helpers.Helper
import io.circe.{HCursor, Json}

/**
 * Transaction of node
 * @param body [[Json]] body of share
 */
case class Share(body: Json)

object Share {
  def apply(cursor: HCursor): Share = {
    new Share(
      Helper.convertToJson(
        s"""
           |{
           |  "pk": "${cursor.downField("pk").as[String].getOrElse("")}",
           |  "w": "${cursor.downField("w").as[String].getOrElse("")}",
           |  "nonce": "${cursor.downField("n").as[String].getOrElse("")}",
           |  "d": "${cursor.downField("d").as[BigInt].getOrElse("")}"
           |}
           |""".stripMargin
      )
    )
  }

  def apply(body: Json): Vector[Share] = {
    val cursor = body.hcursor
    val shares = cursor.values

    if (shares.isEmpty) {
      Vector[Share](Share(cursor))
    }
    else {
      shares.get.toVector.map(item => Share(item.hcursor))
    }
  }
}
