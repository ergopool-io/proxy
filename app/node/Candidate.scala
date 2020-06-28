package node

import helpers.Helper
import io.circe.Json
import loggers.Logger

import scala.math.BigDecimal

class Candidate(miningCandidate: Json,
                client: NodeClient,
                protectedTx: Transaction,
                poolTx: Transaction,
                difficultyFactor: BigDecimal,
                sendTxP: (Transaction, Proof) => Unit) {

  /**
   * Handle the candidate response for proof and response for miner
   *
   * @return
   */
  def getResponse: Json = {
    if (poolTx == null) {
      if (!isTransactionInUnconfirmedTransactions(protectedTx)) {
        Logger.debug(s"Sending transaction to the network: $protectedTx")
        val response = client.sendErgoTransaction(protectedTx)
        if (response.isError) {
          Logger.error(
            s"""
              |Sending Ergo Transaction to network failed:
              |${client.parseErrorResponse(response)}
              |""".stripMargin)
        }
      }
      throw new PoolTxIsNull
    }
    candidate
  }

  /**
   * Get candidate response for the miner
   *
   * @return
   */
  def candidate: Json = {
    if (isTxsExistInProof) {
      if (isMsgChanged) {
        sendTxP(poolTx, Proof(proof, poolTx.id))
      }
      filteredCandidate(miningCandidate)
    }
    else {
      val response = client.candidateWithTxs(Vector[Transaction](protectedTx, poolTx))
      if (response.isSuccess) {
        val body = Helper.ArrayByte(response.body).toJson
        sendTxP(poolTx, Proof(body.hcursor.downField("proof").as[Json].getOrElse(Json.Null), poolTx.id))
        filteredCandidate(body)
      }
      else {
        throw new CandidateWithTxsError(client.parseErrorResponse(response))
      }
    }
  }

  /**
   * Clean /mining/candidate response and put pb in it
   *
   * @return [[String]]
   */
  private def filteredCandidate(body: Json): Json = {
    val cursor = body.hcursor
    val b: BigDecimal = cursor.downField("b").as[BigDecimal].getOrElse(BigDecimal("0"))
    Helper.convertToJson(
      s"""
         |{
         |  "msg": "${cursor.downField("msg").as[String].getOrElse("")}",
         |  "b": $b,
         |  "pk": "${cursor.downField("pk").as[String].getOrElse("")}",
         |  "pb": ${(b * difficultyFactor).toBigInt}
         |}
         |""".stripMargin
    )
  }

  /**
   * Get proof from candidate body
   *
   * @return the proof
   */
  def proof: Json = {
    miningCandidate.hcursor.downField("proof").as[Json].getOrElse(Json.Null)
  }

  /**
   * Check if msg is changed in the candidate body
   *
   * @return true if is changed
   */
  def isMsgChanged: Boolean = {
    miningCandidate.hcursor.downField("msg").as[String].getOrElse(null) != client.blockHeader
  }

  /**
   * Check if both poolTx and protectedTx are in the proof
   *
   * @return true if both exist
   */
  def isTxsExistInProof: Boolean = {
    val txs = proof.hcursor
      .downField("txProofs").as[Json].getOrElse(Json.Null)
      .asArray.getOrElse(Vector[Json]())
    val protectedTxExists = txs.exists(_.hcursor.downField("leaf").as[String].getOrElse(null) == protectedTx.id)
    val poolTxExists = txs.exists(_.hcursor.downField("leaf").as[String].getOrElse(null) == poolTx.id)

    protectedTxExists && poolTxExists
  }

  /**
   * Check if the transaction is in unconfirmed transactions
   *
   * @param transaction the transaction to check its existence
   * @return true if it exists
   */
  def isTransactionInUnconfirmedTransactions(transaction: Transaction): Boolean = {
    var from = 0
    val paginate = 50
    while (true) {
      val response = client.unconfirmedTransactions(from, paginate)

      val txs = Helper.ArrayByte(response.body).toJson.asArray.getOrElse(Vector[Json]())

      if (txs.isEmpty)
        return false

      val isExists = txs.exists(_.hcursor.downField("id").as[String].getOrElse(null) == transaction.id)

      if (isExists)
        return true
      else
        from += paginate
    }
    false // Dummy return statement
  }
}

final class PoolTxIsNull extends Throwable("Pool transaction is null")

final class CandidateWithTxsError(message: String) extends Throwable(message)