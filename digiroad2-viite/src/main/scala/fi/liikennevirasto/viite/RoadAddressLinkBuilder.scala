package fi.liikennevirasto.viite

import fi.liikennevirasto.digiroad2._
import fi.liikennevirasto.digiroad2.asset.LinkGeomSource.{Unknown => _, apply => _}
import fi.liikennevirasto.digiroad2.asset.{LinkGeomSource, _}
import fi.liikennevirasto.digiroad2.client.vvh.{VVHHistoryRoadLink, VVHRoadlink}
import fi.liikennevirasto.digiroad2.linearasset.{RoadLink, RoadLinkLike}
import fi.liikennevirasto.digiroad2.service.RoadLinkType.{apply => _, _}
import fi.liikennevirasto.digiroad2.util.Track
import fi.liikennevirasto.viite.dao._
import fi.liikennevirasto.viite.model.{Anomaly, RoadAddressLink}

object RoadAddressLinkBuilder extends AddressLinkBuilder {

  /* Temporary restriction from PO: Filler limit on modifications
   * (linear location adjustments) is limited to 1 meter. If there is a need to fill /
   * cut more than that then nothing is done to the road address linear location data.
   */
  def build(roadLink: RoadLink, roadAddress: RoadAddress, floating: Boolean = false, newGeometry: Option[Seq[Point]] = None): RoadAddressLink = {
    val roadLinkType = (floating, roadLink.linkSource) match {
      case (true, _) => FloatingRoadLinkType
      case (false, LinkGeomSource.ComplimentaryLinkInterface) => ComplementaryRoadLinkType
      case (false, _) => NormalRoadLinkType
    }
    val geom = GeometryUtils.truncateGeometry3D(roadLink.geometry, roadAddress.startMValue, roadAddress.endMValue)
    val length = GeometryUtils.geometryLength(geom)
    val VVHRoadName = getVVHRoadName(roadLink.attributes)
    val roadName = roadAddress.roadName
    val municipalityCode = roadLink.attributes.getOrElse("MUNICIPALITYCODE", 0).asInstanceOf[Number].intValue()
    val roadType = roadAddress.roadType match {
      case RoadType.Unknown => getRoadType(roadLink.administrativeClass, roadLink.linkType)
      case _ => roadAddress.roadType
    }
    RoadAddressLink(roadAddress.id, roadLink.linkId, geom,
      length, roadLink.administrativeClass, roadLink.linkType, roadLinkType, roadLink.constructionType, roadLink.linkSource, roadType, VVHRoadName, roadName, municipalityCode, extractModifiedAtVVH(roadLink.attributes), Some("vvh_modified"),
      roadLink.attributes, roadAddress.roadNumber, roadAddress.roadPartNumber, roadAddress.track.value, roadAddress.ely, roadAddress.discontinuity.value,
      roadAddress.startAddrMValue, roadAddress.endAddrMValue, roadAddress.startDate.map(formatter.print).getOrElse(""), roadAddress.endDate.map(formatter.print).getOrElse(""), roadAddress.startMValue, roadAddress.endMValue,
      roadAddress.sideCode,
      roadAddress.calibrationPoints._1,
      roadAddress.calibrationPoints._2, Anomaly.None, roadAddress.commonHistoryId, floating = roadAddress.isFloating)

  }

  def build(roadLink: VVHRoadlink, roadAddress: RoadAddress): RoadAddressLink = {
    val roadLinkType = SuravageRoadLinkType
    val geom = GeometryUtils.truncateGeometry3D(roadLink.geometry, roadAddress.startMValue, roadAddress.endMValue)
    val length = GeometryUtils.geometryLength(geom)
    val VVHRoadName = getVVHRoadName(roadLink.attributes)
    val roadName = roadAddress.roadName
    val municipalityCode = roadLink.attributes.getOrElse("MUNICIPALITYCODE", roadLink.municipalityCode).asInstanceOf[Number].intValue()
    val linkType = getLinkType(roadLink)
    val roadType = roadAddress.roadType match {
      case RoadType.Unknown => getRoadType(roadLink.administrativeClass, linkType)
      case _ => roadAddress.roadType
    }

    RoadAddressLink(roadAddress.id, roadLink.linkId, geom,
      length, roadLink.administrativeClass, linkType, roadLinkType, roadLink.constructionType, roadLink.linkSource, roadType, VVHRoadName, roadName, municipalityCode, extractModifiedAtVVH(roadLink.attributes), Some("vvh_modified"),
      roadLink.attributes, roadAddress.roadNumber, roadAddress.roadPartNumber, roadAddress.track.value, roadAddress.ely, roadAddress.discontinuity.value,
      roadAddress.startAddrMValue, roadAddress.endAddrMValue, roadAddress.startDate.map(formatter.print).getOrElse(""), roadAddress.endDate.map(formatter.print).getOrElse(""), roadAddress.startMValue, roadAddress.endMValue,
      roadAddress.sideCode,
      roadAddress.calibrationPoints._1,
      roadAddress.calibrationPoints._2, Anomaly.None, roadAddress.commonHistoryId, floating = roadAddress.isFloating)
  }

  def buildSimpleLink(roadAddress: RoadAddress): RoadAddressLink = {
    val roadLinkType = NormalRoadLinkType
    val geom = roadAddress.geometry
    val length = GeometryUtils.geometryLength(geom)
    val municipalityCode = 0
    val roadType = roadAddress.roadType
    RoadAddressLink(roadAddress.id, roadAddress.linkId, geom,
      length, AdministrativeClass.apply(1), LinkType.apply(99), roadLinkType, ConstructionType.apply(0), LinkGeomSource.apply(1), roadType, Some(""), roadAddress.roadName, municipalityCode, Some(""), Some("vvh_modified"),
      Map(), roadAddress.roadNumber, roadAddress.roadPartNumber, roadAddress.track.value, 0, roadAddress.discontinuity.value,
      roadAddress.startAddrMValue, roadAddress.endAddrMValue, roadAddress.startDate.map(formatter.print).getOrElse(""), roadAddress.endDate.map(formatter.print).getOrElse(""), roadAddress.startMValue, roadAddress.endMValue,
      roadAddress.sideCode,
      roadAddress.calibrationPoints._1,
      roadAddress.calibrationPoints._2, Anomaly.None, roadAddress.commonHistoryId, floating = roadAddress.isFloating)
  }

  def build(roadLink: RoadLinkLike, missingAddress: MissingRoadAddress): RoadAddressLink = {
    roadLink match {
      case rl: RoadLink => buildRoadLink(rl, missingAddress)
      case rl: RoadLinkLike => throw new NotImplementedError(s"No support for building missing road address links on RoadLinkLike subclass ${rl.getClass}")
    }
  }

  private def buildRoadLink(roadLink: RoadLink, missingAddress: MissingRoadAddress): RoadAddressLink = {
    val geom = GeometryUtils.truncateGeometry3D(roadLink.geometry, missingAddress.startMValue.getOrElse(0.0), missingAddress.endMValue.getOrElse(roadLink.length))
    val length = GeometryUtils.geometryLength(geom)
    val roadLinkRoadNumber = roadLink.attributes.get(RoadNumber).map(toIntNumber).getOrElse(0)
    val roadLinkRoadPartNumber = roadLink.attributes.get(RoadPartNumber).map(toIntNumber).getOrElse(0)
    val VVHRoadName = getVVHRoadName(roadLink.attributes)
    val municipalityCode = roadLink.attributes.getOrElse(MunicipalityCode, 0).asInstanceOf[Number].intValue()
    val roadType = missingAddress.roadType match {
      case RoadType.Unknown => getRoadType(roadLink.administrativeClass, roadLink.linkType)
      case _ => missingAddress.roadType
    }
    RoadAddressLink(0, roadLink.linkId, geom,
      length, roadLink.administrativeClass, roadLink.linkType, UnknownRoadLinkType, roadLink.constructionType, roadLink.linkSource, roadType,
      VVHRoadName, Some(""), municipalityCode, extractModifiedAtVVH(roadLink.attributes), Some("vvh_modified"),
      roadLink.attributes, missingAddress.roadNumber.getOrElse(roadLinkRoadNumber),
      missingAddress.roadPartNumber.getOrElse(roadLinkRoadPartNumber), Track.Unknown.value, municipalityRoadMaintainerMapping.getOrElse(roadLink.municipalityCode, -1), Discontinuity.Continuous.value,
      0, 0, "", "", 0.0, length, SideCode.Unknown, None, None, missingAddress.anomaly, newGeometry = Some(roadLink.geometry), floating = false)
  }


  def buildSuravageRoadAddressLink(roadLinkProjectidTuple: (VVHRoadlink, Option[Long])): RoadAddressLink = {
    val roadLink = roadLinkProjectidTuple._1
    val roadAddresses = roadLinkProjectidTuple._2 match { //Check if project attribute has been initialized
      case (Some(projectId)) =>
        ProjectDAO.getProjectLinksByLinkIdAndProjectId(roadLink.linkId, projectId)
      case _ =>
        RoadAddressDAO.fetchByLinkId(Set(roadLink.linkId), includeHistory = false)
    }
    val headAddress = roadAddresses.headOption
    val geom = GeometryUtils.truncateGeometry3D(roadLink.geometry, 0.0, roadLink.length)
    val length = GeometryUtils.geometryLength(geom)
    val sideCode = headAddress match {
      case Some(road) => road.sideCode
      case _ => SideCode.Unknown
    }
    val startAddrM = roadAddresses.nonEmpty match {
      case true => roadAddresses.map(_.startAddrMValue).min
      case false => 0L
    }
    val endAddrM = roadAddresses.nonEmpty match {
      case true => roadAddresses.map(_.endAddrMValue).max
      case false => 0L
    }

    val roadLinkRoadNumber = toLongNumber(headAddress.map(_.roadNumber), roadLink.attributes.get(RoadNumber))
    val roadLinkRoadPartNumber = toLongNumber(headAddress.map(_.roadPartNumber), roadLink.attributes.get(RoadPartNumber))
    val VVHRoadName = getVVHRoadName(roadLink.attributes)
    val municipalityCode = roadLink.municipalityCode
    val roadNames = RoadNameDAO.getLatestRoadName(roadLinkRoadNumber)

    val roadName = if (roadNames.isEmpty) Some("") else Some(roadNames.get.roadName)

    val anomalyType = {
      if (roadLinkRoadNumber != 0 && roadLinkRoadPartNumber != 0) Anomaly.None else Anomaly.NoAddressGiven
    }
    val trackValue = headAddress match {
      case Some(add) =>
        if (add.linkGeomSource == LinkGeomSource.SuravageLinkInterface) {
          add.track.value
        } else {
          roadLink.attributes.getOrElse("TRACK_CODE", Track.Unknown.value).toString.toInt
        }
      case _ => roadLink.attributes.getOrElse("TRACK_CODE", Track.Unknown.value).toString.toInt
    }

    val elyCode: Long = headAddress match {
      case Some(add) => add.ely
      case _ => municipalityRoadMaintainerMapping.getOrElse(roadLink.municipalityCode, -1)
    }
    RoadAddressLink(toLongNumber(headAddress.map(_.id), Some(0)), roadLink.linkId, geom,
      length, roadLink.administrativeClass, getLinkType(roadLink), SuravageRoadLinkType, roadLink.constructionType,
      roadLink.linkSource, getRoadType(roadLink.administrativeClass, getLinkType(roadLink)),
      VVHRoadName, roadName, municipalityCode, extractModifiedAtVVH(roadLink.attributes), Some("vvh_modified"),
      roadLink.attributes, roadLinkRoadNumber, roadLinkRoadPartNumber, trackValue, elyCode, Discontinuity.Continuous.value,
      startAddrM, endAddrM, "", "", 0.0, length, sideCode, None, None, anomalyType, floating = false)
  }

  def build(historyRoadLink: VVHHistoryRoadLink, roadAddress: RoadAddress): RoadAddressLink = {
    val roadLinkType = FloatingRoadLinkType
    val geom = GeometryUtils.truncateGeometry3D(historyRoadLink.geometry, roadAddress.startMValue, roadAddress.endMValue)
    val length = GeometryUtils.geometryLength(geom)
    val VVHRoadName = getVVHRoadName(historyRoadLink.attributes)
    val roadName = roadAddress.roadName
    val municipalityCode = historyRoadLink.attributes.getOrElse(MunicipalityCode, 0).asInstanceOf[Number].intValue()
    val roadType = roadAddress.roadType match {
      case RoadType.Unknown => getRoadType(historyRoadLink.administrativeClass, UnknownLinkType)
      case _ => roadAddress.roadType
    }
    RoadAddressLink(roadAddress.id, historyRoadLink.linkId, geom,
      length, historyRoadLink.administrativeClass, UnknownLinkType, roadLinkType, ConstructionType.UnknownConstructionType, LinkGeomSource.HistoryLinkInterface, roadType, VVHRoadName, roadName, municipalityCode, extractModifiedAtVVH(historyRoadLink.attributes), Some("vvh_modified"),
      historyRoadLink.attributes, roadAddress.roadNumber, roadAddress.roadPartNumber, roadAddress.track.value, roadAddress.ely, roadAddress.discontinuity.value,
      roadAddress.startAddrMValue, roadAddress.endAddrMValue, roadAddress.startDate.map(formatter.print).getOrElse(""), roadAddress.endDate.map(formatter.print).getOrElse(""), roadAddress.startMValue, roadAddress.endMValue,
      roadAddress.sideCode,
      roadAddress.calibrationPoints._1,
      roadAddress.calibrationPoints._2, Anomaly.None, roadAddress.commonHistoryId, floating = roadAddress.isFloating)
  }

  def capToGeometry(geomLength: Double, sourceSegments: Seq[RoadAddressLink]): Seq[RoadAddressLink] = {
    val (overflowingSegments, passThroughSegments) = sourceSegments.partition(x => x.endMValue - MaxAllowedMValueError > geomLength)
    val cappedSegments = overflowingSegments.map { s => s.copy(endMValue = geomLength) }
    passThroughSegments ++ cappedSegments
  }

  def extendToGeometry(geomLength: Double, sourceSegments: Seq[RoadAddressLink]): Seq[RoadAddressLink] = {
    if (sourceSegments.isEmpty)
      return sourceSegments
    val sorted = sourceSegments.sortBy(_.endMValue)(Ordering[Double].reverse)
    val lastSegment = sorted.head
    val restSegments = sorted.tail
    val adjustments = if (lastSegment.endMValue < geomLength - MaxAllowedMValueError) {
      restSegments ++ Seq(lastSegment.copy(endMValue = geomLength))
    } else {
      sourceSegments
    }
    adjustments
  }

  def dropShort(geomLength: Double, sourceSegments: Seq[RoadAddressLink]): Seq[RoadAddressLink] = {
    if (sourceSegments.size < 2)
      return sourceSegments
    val passThroughSegments = sourceSegments.partition(s => s.length >= MinAllowedRoadAddressLength)._1
    passThroughSegments
  }

  def dropSegmentsOutsideGeometry(geomLength: Double, sourceSegments: Seq[RoadAddressLink]): Seq[RoadAddressLink] = {
    val passThroughSegments = sourceSegments.partition(x => x.startMValue + Epsilon <= geomLength)._1
    passThroughSegments
  }

  private def getVVHRoadName(link: Map[String, Any]): Option[String] = {
    Some(link.getOrElse(FinnishRoadName, link.getOrElse(SwedishRoadName, "none")).toString)
  }
}

// TIETYYPPI (1= yleinen tie, 2 = lauttaväylä yleisellä tiellä, 3 = kunnan katuosuus, 4 = yleisen tien työmaa, 5 = yksityistie, 9 = omistaja selvittämättä)
sealed trait RoadType {
  def value: Int

  def displayValue: String
}

object RoadType {
  val values = Set(PublicRoad, FerryRoad, MunicipalityStreetRoad, PublicUnderConstructionRoad, PrivateRoadType, UnknownOwnerRoad)

  def apply(intValue: Int): RoadType = {
    values.find(_.value == intValue).getOrElse(UnknownOwnerRoad)
  }

  case object PublicRoad extends RoadType {
    def value = 1;

    def displayValue = "Yleinen tie"
  }

  case object FerryRoad extends RoadType {
    def value = 2;

    def displayValue = "Lauttaväylä yleisellä tiellä"
  }

  case object MunicipalityStreetRoad extends RoadType {
    def value = 3;

    def displayValue = "Kunnan katuosuus"
  }

  case object PublicUnderConstructionRoad extends RoadType {
    def value = 4;

    def displayValue = "Yleisen tien työmaa"
  }

  case object PrivateRoadType extends RoadType {
    def value = 5;

    def displayValue = "Yksityistie"
  }

  case object UnknownOwnerRoad extends RoadType {
    def value = 9;

    def displayValue = "Omistaja selvittämättä"
  }

  case object Unknown extends RoadType {
    def value = 99;

    def displayValue = "Ei määritelty"
  }

}
