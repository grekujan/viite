package fi.liikennevirasto.digiroad2.linearasset.oracle

import fi.liikennevirasto.digiroad2._
import fi.liikennevirasto.digiroad2.asset.{BoundingRectangle, SideCode}
import fi.liikennevirasto.digiroad2.linearasset.LinearAssetFiller.UnknownLimit
import fi.liikennevirasto.digiroad2.linearasset._
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import org.slf4j.LoggerFactory
import slick.jdbc.{StaticQuery => Q}

class OracleSpeedLimitProvider(eventbus: DigiroadEventBus, roadLinkServiceImplementation: RoadLinkService = RoadLinkService) extends SpeedLimitProvider {
  val dao: OracleLinearAssetDao = new OracleLinearAssetDao {
    override val roadLinkService: RoadLinkService = roadLinkServiceImplementation
  }
  val logger = LoggerFactory.getLogger(getClass)

  def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)
  def withDynSession[T](f: => T): T = OracleDatabase.withDynSession(f)

  override def getUnknown(municipalities: Option[Set[Int]]): Map[String, Map[String, Any]] = {
    withDynSession {
      dao.getUnknownSpeedLimits(municipalities)
    }
  }

  override def purgeUnknown(mmlIds: Set[Long]): Unit = {
    val roadLinks = roadLinkServiceImplementation.fetchVVHRoadlinks(mmlIds)
    withDynTransaction {
      roadLinks.foreach { rl =>
        dao.purgeFromUnknownSpeedLimits(rl.mmlId, GeometryUtils.geometryLength(rl.geometry))
      }
    }
  }

  private def createUnknownLimits(speedLimits: Seq[SpeedLimit], roadLinksByMmlId: Map[Long, VVHRoadLinkWithProperties]): Seq[UnknownLimit] = {
    val generatedLimits = speedLimits.filter(_.id == 0)
    generatedLimits.map { limit =>
      val roadLink = roadLinksByMmlId(limit.mmlId)
      val municipalityCode = RoadLinkUtility.municipalityCodeFromAttributes(roadLink.attributes)
      UnknownLimit(roadLink.mmlId, municipalityCode, roadLink.administrativeClass)
    }
  }

  override def get(bounds: BoundingRectangle, municipalities: Set[Int]): Seq[Seq[SpeedLimit]] = {
    val roadLinks = roadLinkServiceImplementation.getRoadLinksFromVVH(bounds, municipalities)
    withDynTransaction {
      val (speedLimitLinks, topology) = dao.getSpeedLimitLinksByRoadLinks(roadLinks)
      val speedLimits = speedLimitLinks.groupBy(_.mmlId)
      val roadLinksByMmlId = topology.groupBy(_.mmlId).mapValues(_.head)

      val (filledTopology, changeSet) = SpeedLimitFiller.fillTopology(topology, speedLimits)
      eventbus.publish("linearAssets:update", changeSet)

      val unknownLimits = createUnknownLimits(filledTopology, roadLinksByMmlId)
      eventbus.publish("speedLimits:persistUnknownLimits", unknownLimits)

      SpeedLimitPartitioner.partition(filledTopology, roadLinksByMmlId)
    }
  }

  override def get(ids: Seq[Long]): Seq[SpeedLimit] = {
    withDynTransaction {
      ids.flatMap(loadSpeedLimit)
    }
  }

  override def find(speedLimitId: Long): Option[SpeedLimit] = {
    withDynTransaction {
     loadSpeedLimit(speedLimitId)
    }
  }

  private def loadSpeedLimit(speedLimitId: Long): Option[SpeedLimit] = {
    dao.getSpeedLimitLinksById(speedLimitId).headOption
  }

  override def persistUnknown(limits: Seq[UnknownLimit]): Unit = {
    withDynTransaction {
      dao.persistUnknownSpeedLimits(limits)
    }
  }

  override def updateValues(ids: Seq[Long], value: Int, username: String, municipalityValidation: Int => Unit): Seq[Long] = {
    withDynTransaction {
      ids.map(dao.updateSpeedLimitValue(_, value, username, municipalityValidation)).flatten
    }
  }

  override def split(id: Long, splitMeasure: Double, existingValue: Int, createdValue: Int, username: String, municipalityValidation: (Int) => Unit): Seq[SpeedLimit] = {
    withDynTransaction {
      val newId = dao.splitSpeedLimit(id, splitMeasure, createdValue, username, municipalityValidation)
      dao.updateSpeedLimitValue(id, existingValue, username, municipalityValidation)
      Seq(loadSpeedLimit(id).get, loadSpeedLimit(newId).get)
    }
  }

  override def separate(id: Long, valueTowardsDigitization: Int, valueAgainstDigitization: Int, username: String, municipalityValidation: Int => Unit): Seq[SpeedLimit] = {
    withDynTransaction {
      val newId = dao.separateSpeedLimit(id, valueTowardsDigitization, valueAgainstDigitization, username, municipalityValidation)
      Seq(loadSpeedLimit(id).get, loadSpeedLimit(newId).get)
    }
  }

  override def get(municipality: Int): Seq[SpeedLimit] = {
    val roadLinks = roadLinkServiceImplementation.getRoadLinksFromVVH(municipality)
    withDynTransaction {
      val (speedLimitLinks, topology) = dao.getSpeedLimitLinksByRoadLinks(roadLinks)
      val roadLinksByMmlId = topology.groupBy(_.mmlId).mapValues(_.head)

      val (filledTopology, changeSet) = SpeedLimitFiller.fillTopology(topology, speedLimitLinks.groupBy(_.mmlId))
      eventbus.publish("linearAssets:update", changeSet)

      val unknownLimits = createUnknownLimits(filledTopology, roadLinksByMmlId)
      eventbus.publish("speedLimits:persistUnknownLimits", unknownLimits)

      filledTopology
    }
  }

  override def create(newLimits: Seq[NewLimit], value: Int, username: String, municipalityValidation: (Int) => Unit): Seq[Long] = {
    withDynTransaction {
      val createdIds = newLimits.flatMap { limit =>
        dao.createSpeedLimit(username, limit.mmlId, (limit.startMeasure, limit.endMeasure), SideCode.BothDirections, value, municipalityValidation)
      }
      eventbus.publish("speedLimits:purgeUnknownLimits", newLimits.map(_.mmlId).toSet)
      createdIds
    }
  }
}
