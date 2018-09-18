package fi.liikennevirasto.viite.util

import java.util.Properties

import javax.sql.DataSource
import com.jolbox.bonecp.{BoneCPConfig, BoneCPDataSource}
import org.joda.time.format.{ISODateTimeFormat, PeriodFormat}
import slick.driver.JdbcDriver.backend.{Database, DatabaseDef}
import Database.dynamicSession
import _root_.oracle.sql.STRUCT
import fi.liikennevirasto.digiroad2.asset.SideCode
import fi.liikennevirasto.digiroad2.client.vvh.VVHClient
import fi.liikennevirasto.digiroad2.dao.{Queries, SequenceResetterDAO}
import fi.liikennevirasto.digiroad2.linearasset.RoadLinkLike
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.service.RoadLinkService
import fi.liikennevirasto.digiroad2.{DummyEventBus, DummySerializer, GeometryUtils}
import fi.liikennevirasto.viite.dao.{LinearLocationDAO, RoadAddress, RoadAddressDAO}
import fi.liikennevirasto.viite._
import fi.liikennevirasto.viite.process.RoadwayAddressMapper
import org.joda.time.{DateTime, _}
import org.slf4j.LoggerFactory
import slick.driver.JdbcDriver.backend.Database
import slick.jdbc.StaticQuery.interpolation
import slick.jdbc._


object AssetDataImporter {
  sealed trait ImportDataSet {
    def database(): DatabaseDef
  }

  case object TemporaryTables extends ImportDataSet {
    lazy val dataSource: DataSource = {
      val cfg = new BoneCPConfig(OracleDatabase.loadProperties("/import.bonecp.properties"))
      new BoneCPDataSource(cfg)
    }

    def database() = Database.forDataSource(dataSource)
  }

  case object Conversion extends ImportDataSet {
    lazy val dataSource: DataSource = {
      val cfg = new BoneCPConfig(OracleDatabase.loadProperties("/conversion.bonecp.properties"))
      new BoneCPDataSource(cfg)
    }

    def database() = Database.forDataSource(dataSource)
    val roadLinkTable: String = "tielinkki"
    val busStopTable: String = "lineaarilokaatio"
  }

  def humanReadableDurationSince(startTime: DateTime): String = {
    PeriodFormat.getDefault.print(new Period(startTime, DateTime.now()))
  }
}

class AssetDataImporter {
  val logger = LoggerFactory.getLogger(getClass)
  lazy val ds: DataSource = initDataSource

  val Modifier = "dr1conversion"

  def withDynTransaction(f: => Unit): Unit = OracleDatabase.withDynTransaction(f)
  def withDynSession[T](f: => T): T = OracleDatabase.withDynSession(f)

  implicit object SetStruct extends SetParameter[STRUCT] {
    def apply(v: STRUCT, pp: PositionedParameters) {
      pp.setObject(v, java.sql.Types.STRUCT)
    }
  }

  def time[A](f: => A) = {
    val s = System.nanoTime
    val ret = f
    println("time for insert " + (System.nanoTime - s) / 1e6 + "ms")
    ret
  }

  val dateFormatter = ISODateTimeFormat.basicDate()

  def getBatchDrivers(n: Int, m: Int, step: Int): List[(Int, Int)] = {
    if ((m - n) < step) {
      List((n, m))
    } else {
      val x = (n to m by step).sliding(2).map(x => (x(0), x(1) - 1)).toList
      x :+ (x.last._2 + 1, m)
    }
  }

  case class RoadTypeChangePoints(roadNumber: Long, roadPartNumber: Long, addrM: Long, before: RoadType, after: RoadType, elyCode: Long)

  /**
    * Get road type for road address object with a list of road type change points
    *
    * @param changePoints Road part change points for road types
    * @param roadAddress Road address to get the road type for
    * @return road type for the road address or if a split is needed then a split point (address) and road types for first and second split
    */
  def roadType(changePoints: Seq[RoadTypeChangePoints], roadAddress: RoadAddress): Either[RoadType, (Long, RoadType, RoadType)] = {
    // Check if this road address overlaps the change point and needs to be split
    val overlaps = changePoints.find(c => c.addrM > roadAddress.startAddrMValue && c.addrM < roadAddress.endAddrMValue)
    if (overlaps.nonEmpty)
      Right((overlaps.get.addrM, overlaps.get.before, overlaps.get.after))
    else {
      // There is no overlap, check if this road address is between [0, min(addrM))
      if (roadAddress.startAddrMValue < changePoints.map(_.addrM).min) {
        Left(changePoints.minBy(_.addrM).before)
      } else {
        Left(changePoints.filter(_.addrM <= roadAddress.startAddrMValue).maxBy(_.addrM).after)
      }
    }

  }

  def importRoadAddressData(conversionDatabase: DatabaseDef, vvhClient: VVHClient,
                            importOptions: ImportOptions): Unit = {

    withDynTransaction {
      sqlu"""ALTER TABLE ROAD_ADDRESS DISABLE ALL TRIGGERS""".execute
      sqlu"""DELETE FROM PROJECT_LINK_NAME""".execute
      sqlu"""DELETE FROM PROJECT_LINK""".execute
      sqlu"""DELETE FROM PROJECT_LINK_HISTORY""".execute
      sqlu"""DELETE FROM PROJECT_RESERVED_ROAD_PART""".execute
      sqlu"""DELETE FROM PROJECT""".execute
      sqlu"""DELETE FROM ROAD_NETWORK_ERROR""".execute
      sqlu"""DELETE FROM PUBLISHED_ROAD_ADDRESS""".execute
      sqlu"""DELETE FROM PUBLISHED_ROAD_NETWORK""".execute
      sqlu"""DELETE FROM ROAD_ADDRESS""".execute
      sqlu"""DELETE FROM LINEAR_LOCATION""".execute
      sqlu"""DELETE FROM ROAD_ADDRESS_CHANGES""".execute
      println(s"${DateTime.now()} - Old address data removed")

      val roadAddressImporter = getRoadAddressImporter(conversionDatabase, vvhClient, importOptions)
      roadAddressImporter.importRoadAddress()

      println(s"${DateTime.now()} - Updating geometry adjustment timestamp to ${importOptions.geometryAdjustedTimeStamp}")
      sqlu"""UPDATE LINEAR_LOCATION
        SET ADJUSTED_TIMESTAMP = ${importOptions.geometryAdjustedTimeStamp}""".execute

      println(s"${DateTime.now()} - Updating calibration point information")
      // both dates are open-ended or there is overlap (checked with inverse logic)
      /*sqlu"""UPDATE ROAD_ADDRESS
        SET CALIBRATION_POINTS = CASE
                                    WHEN CALIBRATION_POINTS = 2 THEN 3
                                    WHEN CALIBRATION_POINTS = 3 THEN 3
                                    ELSE 1
                                  END
        WHERE NOT EXISTS(SELECT 1 FROM ROAD_ADDRESS RA2 WHERE RA2.ID != ROAD_ADDRESS.ID AND
        RA2.ROAD_NUMBER = ROAD_ADDRESS.ROAD_NUMBER AND
        RA2.ROAD_PART_NUMBER = ROAD_ADDRESS.ROAD_PART_NUMBER AND
        RA2.START_ADDR_M = ROAD_ADDRESS.END_ADDR_M AND
        RA2.COMMON_HISTORY_ID = ROAD_ADDRESS.COMMON_HISTORY_ID AND
        RA2.TRACK_CODE = ROAD_ADDRESS.TRACK_CODE AND
        (ROAD_ADDRESS.END_DATE IS NULL AND RA2.END_DATE IS NULL OR
        NOT (RA2.END_DATE < ROAD_ADDRESS.START_DATE OR RA2.START_DATE > ROAD_ADDRESS.END_DATE)))""".execute
      sqlu"""UPDATE ROAD_ADDRESS
        SET CALIBRATION_POINTS = CASE
                                    WHEN CALIBRATION_POINTS = 2 THEN 2
                                    WHEN CALIBRATION_POINTS = 3 THEN 3
                                    ELSE CALIBRATION_POINTS + 2
                                  END
          WHERE
            START_ADDR_M = 0 OR
            NOT EXISTS(SELECT 1 FROM ROAD_ADDRESS RA2 WHERE RA2.ID != ROAD_ADDRESS.ID AND
              RA2.ROAD_NUMBER = ROAD_ADDRESS.ROAD_NUMBER AND
              RA2.ROAD_PART_NUMBER = ROAD_ADDRESS.ROAD_PART_NUMBER AND
              RA2.END_ADDR_M = ROAD_ADDRESS.START_ADDR_M AND
              RA2.TRACK_CODE = ROAD_ADDRESS.TRACK_CODE AND
              RA2.COMMON_HISTORY_ID = ROAD_ADDRESS.COMMON_HISTORY_ID AND
              (ROAD_ADDRESS.END_DATE IS NULL AND RA2.END_DATE IS NULL OR
                NOT (RA2.END_DATE < ROAD_ADDRESS.START_DATE OR RA2.START_DATE > ROAD_ADDRESS.END_DATE)
              )
            )""".execute*/
      sqlu"""ALTER TABLE ROAD_ADDRESS ENABLE ALL TRIGGERS""".execute
      roadwayResetter()
    }
  }

  def roadwayResetter(): Unit = {
    val sequenceResetter = new SequenceResetterDAO()
    sql"""select MAX(roadway_id) FROM ROAD_ADDRESS""".as[Long].firstOption match {
      case Some(roadwayId) =>
        sequenceResetter.resetSequenceToNumber("ROADWAY_SEQ", roadwayId + 1)
      case _ => sequenceResetter.resetSequenceToNumber("ROADWAY_SEQ", 1)
    }
  }

  protected def getRoadAddressImporter(conversionDatabase: DatabaseDef, vvhClient: VVHClient, importOptions: ImportOptions) = {
    new RoadAddressImporter(conversionDatabase, vvhClient, importOptions)
  }

  def splitRoadAddresses(roadAddress: RoadAddress, addrMToSplit: Long, roadTypeBefore: RoadType, roadTypeAfter: RoadType, elyCode: Long): Seq[RoadAddress] = {
    // mValue at split point on a TowardsDigitizing road address:
    val splitMValue = roadAddress.startMValue + (roadAddress.endMValue - roadAddress.startMValue) / (roadAddress.endAddrMValue - roadAddress.startAddrMValue) * (addrMToSplit - roadAddress.startAddrMValue)
    println(s"Splitting road address id = ${roadAddress.id}, tie = ${roadAddress.roadNumber} and aosa = ${roadAddress.roadPartNumber}, on AddrMValue = $addrMToSplit")
    val roadAddressA = roadAddress.copy(id = fi.liikennevirasto.viite.NewRoadAddress, roadType = roadTypeBefore, endAddrMValue = addrMToSplit, startMValue = if (roadAddress.sideCode == SideCode.AgainstDigitizing)
            roadAddress.endMValue - splitMValue
          else
            0.0, endMValue = if (roadAddress.sideCode == SideCode.AgainstDigitizing)
            roadAddress.endMValue
          else
            splitMValue, geometry = GeometryUtils.truncateGeometry2D(roadAddress.geometry, 0.0, splitMValue), ely = elyCode) // TODO Check common_history_id

    val roadAddressB = roadAddress.copy(id = fi.liikennevirasto.viite.NewRoadAddress, roadType = roadTypeAfter, startAddrMValue = addrMToSplit, startMValue = if (roadAddress.sideCode == SideCode.AgainstDigitizing)
            0.0
          else
            splitMValue, endMValue = if (roadAddress.sideCode == SideCode.AgainstDigitizing)
            roadAddress.endMValue - splitMValue
          else
            roadAddress.endMValue, geometry = GeometryUtils.truncateGeometry2D(roadAddress.geometry, splitMValue, roadAddress.endMValue), ely = elyCode) // TODO Check common_history_id
    Seq(roadAddressA, roadAddressB)
  }

  def updateRoadWithSingleRoadType(roadNumber:Long, roadPartNumber: Long, roadType : Long, elyCode :Long) = {
    println(s"Updating road number $roadNumber and part $roadPartNumber with roadType = $roadType and elyCode = $elyCode")
    sqlu"""UPDATE ROAD_ADDRESS SET ROAD_TYPE = ${roadType}, ELY= ${elyCode} where ROAD_NUMBER = ${roadNumber} AND ROAD_PART_NUMBER = ${roadPartNumber} """.execute
  }

  def updateMissingRoadAddresses(vvhClient: VVHClient) = {
    val roadNumbersToFetch = Seq((1, 19999), (40000,49999))
    val eventBus = new DummyEventBus
    val linkService = new RoadLinkService(vvhClient, eventBus, new DummySerializer)
    val service = new RoadAddressService(linkService, new RoadwayAddressMapper(new RoadAddressDAO()), eventBus)
    RoadAddressLinkBuilder.municipalityMapping               // Populate it beforehand, because it can't be done in nested TX
    RoadAddressLinkBuilder.municipalityRoadMaintainerMapping // Populate it beforehand, because it can't be done in nested TX
    val municipalities = OracleDatabase.withDynTransaction {
      sqlu"""DELETE FROM MISSING_ROAD_ADDRESS""".execute
      println("Old address data cleared")
      Queries.getMunicipalitiesWithoutAhvenanmaa
    }
      municipalities.foreach(municipality => {
        println("Processing municipality %d at time: %s".format(municipality, DateTime.now().toString))
        val missing = service.getMissingRoadAddresses(roadNumbersToFetch, municipality)
        println("Got %d links".format(missing.size))
        service.createMissingRoadAddress(missing)
        println("Municipality %d: %d links added at time: %s".format(municipality, missing.size, DateTime.now().toString))
      })
  }

  private def generateChunks(linkIds: Seq[Long], chunkNumber: Long): Seq[(Long, Long)] = {
    val (chunks, _) = linkIds.foldLeft((Seq[Long](0), 0)) {
      case ((fchunks, index), linkId) =>
        if (index > 0 && index % chunkNumber == 0) {
          (fchunks ++ Seq(linkId), index + 1)
        } else {
          (fchunks, index + 1)
        }
    }
    val result = if (chunks.last == linkIds.last) {
      chunks
    } else {
      chunks ++ Seq(linkIds.last)
    }

    result.zip(result.tail)
  }

  protected def fetchChunkLinkIds(): Seq[(Long, Long)] = {
      val linkIds = sql"""select distinct link_id from linear_location where link_id is not null order by link_id""".as[Long].list
      generateChunks(linkIds, 25000l)
    }

  def updateRoadAddressesGeometry(vvhClient: VVHClient, customFilter: String = ""): Unit = {
    val eventBus = new DummyEventBus
    val linkService = new RoadLinkService(vvhClient, eventBus, new DummySerializer)
    var changed = 0
    withDynTransaction {
        val chunks = fetchChunkLinkIds()
        chunks.foreach {
        case (min, max) =>
        val linkIds = LinearLocationDAO.fetchLinkIdsInChunk(min, max).toSet
        val roadLinksFromVVH = linkService.getCurrentAndComplementaryAndSuravageRoadLinksFromVVH(linkIds)
        val unGroupedTopology = LinearLocationDAO.fetchByLinkId(roadLinksFromVVH.map(_.linkId).toSet, false)
        val topologyLocation = unGroupedTopology.groupBy(_.linkId)
        val isLoopOrEmptyGeom = if (unGroupedTopology.sortBy(_.orderNumber).flatMap(_.geometry).equals(Nil)) {
          true
        } else GeometryUtils.isLoopGeometry(unGroupedTopology.sortBy(_.orderNumber).flatMap(_.geometry))

        roadLinksFromVVH.foreach(roadLink => {
          val segmentsOnViiteDatabase = topologyLocation.getOrElse(roadLink.linkId, Set())
          segmentsOnViiteDatabase.foreach(segment => {
            val newGeom = GeometryUtils.truncateGeometry3D(roadLink.geometry, segment.startMValue, segment.endMValue)
            if (!segment.geometry.equals(Nil) && !newGeom.equals(Nil)) {
              val distanceFromHeadToHead = segment.geometry.head.distance2DTo(newGeom.head)
              val distanceFromHeadToLast = segment.geometry.head.distance2DTo(newGeom.last)
              val distanceFromLastToHead = segment.geometry.last.distance2DTo(newGeom.head)
              val distanceFromLastToLast = segment.geometry.last.distance2DTo(newGeom.last)
              if (((distanceFromHeadToHead > MinDistanceForGeometryUpdate) &&
                (distanceFromHeadToLast > MinDistanceForGeometryUpdate)) ||
                ((distanceFromLastToHead > MinDistanceForGeometryUpdate) &&
                  (distanceFromLastToLast > MinDistanceForGeometryUpdate)) ||
                isLoopOrEmptyGeom) {
                LinearLocationDAO.updateGeometry(segment.id, newGeom)
                println("Changed geometry on roadAddress id " + segment.id + " and linkId =" + segment.linkId)
                changed += 1
              } else {
                println(s"Skipped geometry update on Road Address ID : ${segment.id} and linkId: ${segment.linkId}")
              }
            }
          })
        })
      }
      println(s"Geometries changed count: $changed")
    }
  }

  private[this] def initDataSource: DataSource = {
    Class.forName("oracle.jdbc.driver.OracleDriver")
    val cfg = new BoneCPConfig(localProperties)
    new BoneCPDataSource(cfg)
  }

  lazy val localProperties: Properties = {
    val props = new Properties()
    try {
      props.load(getClass.getResourceAsStream("/bonecp.properties"))
    } catch {
      case e: Exception => throw new RuntimeException("Can't load local.properties for env: " + System.getProperty("env"), e)
    }
    props
  }

}

case class ImportOptions(onlyComplementaryLinks: Boolean, useFrozenLinkService: Boolean, geometryAdjustedTimeStamp: Long, conversionTable: String, onlyCurrentRoads: Boolean)
case class RoadPart(roadNumber: Long, roadPart: Long, ely: Long)

