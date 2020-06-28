package models

import java.sql.Connection

import io.ebean.{Ebean, Finder}

import scala.collection.JavaConverters._

class BlockFinder extends Finder[Long, Block](classOf[Block]) {
  /**
   * Get list of main chain blocks, sorted in descending height
   *
   * @return list of main chain blocks, sorted in descending height
   */
  def sortedMainChainBlocks: Vector[Block] = {
    this.query()
      .orderBy().desc("height")
      .findList()
      .asScala
      .toVector
  }

  /**
   * Find block by its block id
   *
   * @param blockId block id to be searched
   * @return the block, null if not found
   */
  def byBlockId(blockId: String): Block = {
    this.query().where().eq("blockId", blockId).findOne()
  }

  /**
   * Get maximum height between blocks
   * @return maximum height, 0 if table is empty
   */
  def maxHeight(): Int = {
    this.query().select("max(height)")
      .findSingleAttribute[Int]()
  }

  /**
   * Remove old unused blocks that have height less than (currentHeight - 1440)
   *
   * @param currentHeight current height
   */
  def removeOldUnusedBlocks(currentHeight: Int): Unit = {
    val c: Connection = Ebean.currentTransaction().getConnection
    val stmt = c.createStatement()
    stmt.execute(s"""
       |delete from block where height < ${currentHeight - 1440}
       |    and not exists(select bo.id from box bo
       |    where bo.spent_in_id = block.id or bo.created_in_id = block.id)
       |""".stripMargin)
  }

  /**
   * Mark the blocks that are having height greater than the minHeight
   *
   * @param forkedHeight the minimum height of blocks
   */
  def deleteForked(forkedHeight: Int): Unit = {
    this.query().where().gt("height", forkedHeight).delete()
  }
}
