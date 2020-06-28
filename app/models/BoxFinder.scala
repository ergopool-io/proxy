package models

import io.ebean.{Finder, RawSqlBuilder}
import loggers.Logger

import scala.collection.JavaConverters._

class BoxFinder extends Finder[String, Box](classOf[Box]) {
  /**
   * Remove spent boxes that have height less than currentHeight
   *
   * @param currentHeight the current height
   */
  def removeOldSpentBoxes(currentHeight: Int): Unit = {
    this.query()
      .where().isNotNull("spentIn")
      .where().lt("spentIn.height", currentHeight - 1440)
      .delete()
  }

  /**
   * Get box by its id
   *
   * @param id the id of box
   * @return a box
   */
  def byBoxId(id: String): Box = {
    this.query().where().eq("id", id).findOne()
  }

  /**
   * Check if a box exists
   *
   * @param id the id of the box
   * @return true if exists, false o.w.
   */
  def boxExists(id: String): Boolean = {
    byId(id) != null
  }

  /**
   * Get unspent boxes from DB
   *
   * @param total total value sum of boxes
   * @param include list of allowed addresses for boxes
   * @param exclude list of disallowed addresses for boxes
   * @return list of unspent boxes
   */
  def unspentBoxesWithTotalValue(
      total: Long,
      include: Vector[String] = Vector[String](),
      exclude: Vector[String] = Vector[String](),
      maxHeight: Int = 0): Vector[Box] = {
    try {
      val raw = RawSqlBuilder
        .parse(
          s"""
             |select id, value, address, ergo_tree, transaction_id, spent_in_id, created_in_id
             |from (
             |    select box.*, SUM(value) OVER(ORDER BY creation_height ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING) AS total
             |    from box
             |    where
             |         spent_in_id is null ${if (maxHeight != 0) "AND inclusion_height <= " + maxHeight else ""}
             |         ${if (include.nonEmpty) s"""AND address IN (${include.mkString(start = "'", sep = "', '", end = "'")})"""
                        else if (exclude.nonEmpty) s"""AND address NOT IN (${exclude.mkString(start = "'", sep = "', '", end = "'")})"""
                        else ""}
             |) T
             |where T.total < $total or T.total is null
             |""".stripMargin
        )
        .create()
      val boxes = this.query().setRawSql(raw).findList().asScala.toVector
      if (boxes.foldLeft(0L)(_ + _.value) < total)
        Vector[Box]()
      else boxes
    }
    catch {
      case e: java.lang.IllegalArgumentException =>
        Logger.error("exception in getting boxes", e)
        Vector[Box]()
    }
  }

  /**
   * Get boxes that have their id is in the list
   *
   * @param include the list of ids
   * @return number of boxes available in db from include vector
   */
  def getAvailableBoxesCount(include: Vector[String]): Int = {
    try {
      val raw = RawSqlBuilder
        .parse(
          s"""
             |select id, value, address, ergo_tree, transaction_id, spent_in_id, created_in_id
             |from (
             |    select box.*
             |    from box
             |    where
             |         spent_in_id is null AND
             |         ${s"""box.id IN (${include.mkString(start = "'", sep = "', '", end = "'")})"""}
             |) T
             |""".stripMargin
        )
        .create()
      this.query().setRawSql(raw).findCount()
    }
    catch {
      case e: java.lang.IllegalArgumentException =>
        Logger.error("exception in getting boxes", e)
        0
    }
  }
}
