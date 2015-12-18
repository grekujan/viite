package fi.liikennevirasto.digiroad2.pointasset.oracle

import fi.liikennevirasto.digiroad2.{IncomingObstacle, IncomingPointAsset, Point, PersistedPointAsset}
import fi.liikennevirasto.digiroad2.asset.oracle.{Sequences, Queries}
import fi.liikennevirasto.digiroad2.asset.oracle.Queries._
import org.joda.time.DateTime
import slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession
import slick.jdbc.{GetResult, PositionedResult, StaticQuery}
import slick.jdbc.StaticQuery.interpolation

case class Obstacle(id: Long, mmlId: Long,
                    lon: Double, lat: Double,
                    mValue: Double, floating: Boolean,
                    municipalityCode: Int,
                    obstacleType: Int,
                    createdBy: Option[String] = None,
                    createdAt: Option[DateTime] = None,
                    modifiedBy: Option[String] = None,
                    modifiedAt: Option[DateTime] = None) extends PersistedPointAsset

object OracleObstacleDao {
  // This works as long as there is only one (and exactly one) property (currently type) for obstacles and up to one value
  def fetchByFilter(queryFilter: String => String): Seq[Obstacle] = {
    val query =
      """
        select a.id, pos.mml_id, a.geometry, pos.start_measure, a.floating, a.municipality_code, ev.value, a.created_by, a.created_date, a.modified_by, a.modified_date
        from asset a
        join asset_link al on a.id = al.asset_id
        join lrm_position pos on al.position_id = pos.id
        join property p on p.asset_type_id = a.asset_type_id
        left join single_choice_value scv on scv.asset_id = a.id
        left join enumerated_value ev on (ev.property_id = p.id AND scv.enumerated_value_id = ev.id)
      """
    val queryWithFilter = queryFilter(query) + " and (a.valid_to > sysdate or a.valid_to is null)"
    StaticQuery.queryNA[Obstacle](queryWithFilter).iterator.toSeq
  }

  implicit val getPointAsset = new GetResult[Obstacle] {
    def apply(r: PositionedResult) = {
      val id = r.nextLong()
      val mmlId = r.nextLong()
      val point = r.nextBytesOption().map(bytesToPoint).get
      val mValue = r.nextDouble()
      val floating = r.nextBoolean()
      val municipalityCode = r.nextInt()
      val obstacleType = r.nextInt()
      val createdBy = r.nextStringOption()
      val createdDateTime = r.nextTimestampOption().map(timestamp => new DateTime(timestamp))
      val modifiedBy = r.nextStringOption()
      val modifiedDateTime = r.nextTimestampOption().map(timestamp => new DateTime(timestamp))

      Obstacle(id, mmlId, point.x, point.y, mValue, floating, municipalityCode, obstacleType, createdBy, createdDateTime, modifiedBy, modifiedDateTime)
    }
  }

  def create(obstacle: IncomingObstacle, mValue: Double, username: String, municipality: Int): Long = {
    val id = Sequences.nextPrimaryKeySeqValue
    val lrmPositionId = Sequences.nextLrmPositionPrimaryKeySeqValue
    sqlu"""
      insert all
        into asset(id, asset_type_id, created_by, created_date, municipality_code)
        values ($id, 220, $username, sysdate, $municipality)

        into lrm_position(id, start_measure, mml_id)
        values ($lrmPositionId, $mValue, ${obstacle.mmlId})

        into asset_link(asset_id, position_id)
        values ($id, $lrmPositionId)

      select * from dual
    """.execute
    updateAssetGeometry(id, Point(obstacle.lon, obstacle.lat))
    insertSingleChoiceProperty(id, getPropertyId, obstacle.obstacleType).execute
    id
  }

  def update(id: Long, obstacle: IncomingObstacle, mValue: Double, username: String, municipality: Int) = {
    sqlu""" update asset set municipality_code = $municipality where id = $id """.execute
    updateAssetModified(id, username).execute
    updateAssetGeometry(id, Point(obstacle.lon, obstacle.lat))
    updateSingleChoiceProperty(id, getPropertyId, obstacle.obstacleType).execute

    sqlu"""
      update lrm_position
       set
       start_measure = $mValue,
       mml_id = ${obstacle.mmlId}
       where id = (select position_id from asset_link where asset_id = $id)
    """.execute
    id
  }

  private def getPropertyId: Long = {
    StaticQuery.query[String, Long](Queries.propertyIdByPublicId).apply("esterakennelma").first
  }
}



