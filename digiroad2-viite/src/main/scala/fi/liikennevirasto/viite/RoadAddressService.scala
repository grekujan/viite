package fi.liikennevirasto.viite

import fi.liikennevirasto.digiroad2.asset.{BoundingRectangle, SideCode, State}
import fi.liikennevirasto.digiroad2.linearasset.RoadLink
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.util.Track
import fi.liikennevirasto.digiroad2.{DigiroadEventBus, GeometryUtils, RoadLinkService, VVHRoadlink}
import fi.liikennevirasto.viite.dao._
import fi.liikennevirasto.viite.model.{Anomaly, RoadAddressLink}
import fi.liikennevirasto.viite.process.RoadAddressFiller
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import slick.jdbc.{StaticQuery => Q}

import scala.collection.mutable.ListBuffer

class RoadAddressService(roadLinkService: RoadLinkService, eventbus: DigiroadEventBus) {

  def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)

  val logger = LoggerFactory.getLogger(getClass)

  val HighwayClass = 1
  val MainRoadClass = 2
  val RegionalClass = 3
  val ConnectingClass = 4
  val MinorConnectingClass = 5
  val StreetClass = 6
  val RampsAndRoundAboutsClass = 7
  val PedestrianAndBicyclesClass = 8
  val WinterRoadsClass = 9
  val PathsClass = 10
  val ConstructionSiteTemporaryClass = 11
  val NoClass = 99

  class Contains(r: Range) {
    def unapply(i: Int): Boolean = r contains i
  }

  /**
    * Get calibration points for road not in a project
    *
    * @param roadNumber
    * @return
    */
  def getCalibrationPoints(roadNumber: Long) = {
    // TODO: Implementation
    Seq(CalibrationPoint(1, 0.0, 0))
  }

  /**
    * Get calibration points for road including project created ones
    *
    * @param roadNumber
    * @param projectId
    * @return
    */
  def getCalibrationPoints(roadNumber: Long, projectId: Long): Seq[CalibrationPoint] = {
    // TODO: Implementation
    getCalibrationPoints(roadNumber) ++ Seq(CalibrationPoint(2, 0.0, 0))
  }

  def getCalibrationPoints(linkIds: Set[Long]) = {

    linkIds.map(linkId => CalibrationPoint(linkId, 0.0, 0))
  }

  def addRoadAddresses(roadLinks: Seq[RoadLink]) = {
    val linkIds = roadLinks.map(_.linkId).toSet
    val calibrationPoints = getCalibrationPoints(linkIds)

  }

  def getRoadAddressLinks(boundingRectangle: BoundingRectangle, roadNumberLimits: Seq[(Int, Int)], municipalities: Set[Int], everything: Boolean = false) = {
    val roadLinks = roadLinkService.getViiteRoadLinksFromVVH(boundingRectangle, roadNumberLimits, municipalities, everything)
    val linkIds = roadLinks.map(_.linkId).toSet
    val addresses = withDynTransaction {
      RoadAddressDAO.fetchByLinkId(linkIds).groupBy(_.linkId)
    }
    val missingLinkIds = linkIds -- addresses.keySet
    val missedRL = withDynTransaction {
      RoadAddressDAO.getMissingRoadAddresses(missingLinkIds)
    }.groupBy(_.linkId)

    val viiteRoadLinks = roadLinks.map { rl =>
      val ra = addresses.getOrElse(rl.linkId, Seq())
      val missed = missedRL.getOrElse(rl.linkId, Seq())
      rl.linkId -> buildRoadAddressLink(rl, ra, missed)
    }.toMap

    val (filledTopology, changeSet) = RoadAddressFiller.fillTopology(roadLinks, viiteRoadLinks)

    eventbus.publish("roadAddress:persistMissingRoadAddress", changeSet.missingRoadAddresses)

    filledTopology
  }

  /**
    * Returns missing road addresses for links that did not already exist in database
    *
    * @param roadNumberLimits
    * @param municipality
    * @return
    */
  def getMissingRoadAddresses(roadNumberLimits: Seq[(Int, Int)], municipality: Int) = {
    val roadLinks = roadLinkService.getViiteRoadLinksFromVVH(municipality, roadNumberLimits)
    val linkIds = roadLinks.map(_.linkId).toSet
    val addresses = RoadAddressDAO.fetchByLinkId(linkIds).groupBy(_.linkId)

    val missingLinkIds = linkIds -- addresses.keySet
    val missedRL = RoadAddressDAO.getMissingRoadAddresses(missingLinkIds).groupBy(_.linkId)

    val viiteRoadLinks = roadLinks.map { rl =>
      val ra = addresses.getOrElse(rl.linkId, Seq())
      val missed = missedRL.getOrElse(rl.linkId, Seq())
      rl.linkId -> buildRoadAddressLink(rl, ra, missed)
    }.toMap

    val (_, changeSet) = RoadAddressFiller.fillTopology(roadLinks, viiteRoadLinks)

    changeSet.missingRoadAddresses
  }

  def buildRoadAddressLink(rl: RoadLink, roadAddrSeq: Seq[RoadAddress], missing: Seq[MissingRoadAddress]): Seq[RoadAddressLink] = {
    roadAddrSeq.map(ra => {
      RoadAddressLinkBuilder.build(rl, ra)
    }).filter(_.length > 0.0) ++
      missing.map(m => RoadAddressLinkBuilder.build(rl, m)).filter(_.length > 0.0)
  }

  def getRoadParts(boundingRectangle: BoundingRectangle, roadNumberLimits: Seq[(Int, Int)], municipalities: Set[Int]) = {
    val addresses = withDynTransaction {
      RoadAddressDAO.fetchPartsByRoadNumbers(roadNumberLimits).groupBy(_.linkId)
    }
    val roadLinks = roadLinkService.getViiteRoadPartsFromVVH(addresses.keySet, municipalities)
    roadLinks.flatMap { rl =>
      val ra = addresses.getOrElse(rl.linkId, List())
      buildRoadAddressLink(rl, ra, Seq())
    }
  }

  def getCoarseRoadParts(boundingRectangle: BoundingRectangle, roadNumberLimits: Seq[(Int, Int)], municipalities: Set[Int]) = {
    val addresses = withDynTransaction {
      RoadAddressDAO.fetchPartsByRoadNumbers(roadNumberLimits, coarse = true).groupBy(_.linkId)
    }
    val roadLinks = roadLinkService.getViiteRoadPartsFromVVH(addresses.keySet, municipalities)
    val groupedLinks = roadLinks.flatMap { rl =>
      val ra = addresses.getOrElse(rl.linkId, List())
      buildRoadAddressLink(rl, ra, Seq())
    }.groupBy(_.roadNumber)

    val retval = groupedLinks.mapValues {
      case (viiteRoadLinks) =>
        val sorted = viiteRoadLinks.sortWith({
          case (ral1, ral2) =>
            if (ral1.roadNumber < ral2.roadNumber)
              true
            else if (ral1.roadNumber > ral2.roadNumber)
              false
            else if (ral1.roadPartNumber < ral2.roadPartNumber)
              true
            else if (ral1.roadPartNumber > ral2.roadPartNumber)
              false
            else if (ral1.startAddressM < ral2.startAddressM)
              true
            else
              false
        })
        sorted.zip(sorted.tail).map {
          case (st1, st2) =>
            st1.copy(geometry = Seq(st1.geometry.head, st2.geometry.head))
        }
    }
    retval.flatMap(x => x._2).toSeq
  }

  def roadClass(roadAddressLink: RoadAddressLink) = {
    val C1 = new Contains(1 to 39)
    val C2 = new Contains(40 to 99)
    val C3 = new Contains(100 to 999)
    val C4 = new Contains(1000 to 9999)
    val C5 = new Contains(10000 to 19999)
    val C6 = new Contains(40000 to 49999)
    val C7 = new Contains(20001 to 39999)
    val C8a = new Contains(70001 to 89999)
    val C8b = new Contains(90001 to 99999)
    val C9 = new Contains(60001 to 61999)
    val C10 = new Contains(62001 to 62999)
    val C11 = new Contains(9900 to 9999)
    try {
      val roadNumber: Int = roadAddressLink.roadNumber.toInt
      roadNumber match {
        case C1() => HighwayClass
        case C2() => MainRoadClass
        case C3() => RegionalClass
        case C4() => ConnectingClass
        case C5() => MinorConnectingClass
        case C6() => StreetClass
        case C7() => RampsAndRoundAboutsClass
        case C8a() => PedestrianAndBicyclesClass
        case C8b() => PedestrianAndBicyclesClass
        case C9() => WinterRoadsClass
        case C10() => PathsClass
        case C11() => ConstructionSiteTemporaryClass
        case _ => NoClass
      }
    } catch {
      case ex: NumberFormatException => NoClass
    }
  }

  def createMissingRoadAddress(missingRoadLinks: Seq[MissingRoadAddress]) = {
    withDynTransaction {
      missingRoadLinks.foreach(createSingleMissingRoadAddress)
    }
  }

  def createSingleMissingRoadAddress(missingAddress: MissingRoadAddress) = {
    RoadAddressDAO.createMissingRoadAddress(missingAddress)
  }

  //TODO: Priority order for anomalies. Currently this is unused
  def getAnomalyCodeByLinkId(linkId: Long, roadPart: Long): Anomaly = {
    //roadlink dont have road address
    if (RoadAddressDAO.getLrmPositionByLinkId(linkId).length < 1) {
      return Anomaly.NoAddressGiven
    }
    //road address do not cover the whole length of the link
    //TODO: Check against link geometry length, using tolerance and GeometryUtils that we cover the whole link
    else if (RoadAddressDAO.getLrmPositionMeasures(linkId).map(x => Math.abs(x._3 - x._2)).sum > 0) {
      return Anomaly.NotFullyCovered
    }
    //road address having road parts that dont matching
    else if (RoadAddressDAO.getLrmPositionRoadParts(linkId, roadPart).nonEmpty) {
      return Anomaly.Illogical
    }
    Anomaly.None
  }

  /**
    * Method that coordinates the cycle for finding and marking the Floating Road Addresses
    *
    * @param batchSize The amount of road addresses to be pulled each time
    */
  def markFloatingRoadAddresses(batchSize: Long): Unit = {
    var roadAddressAmount: Long = 0
    var nonProcessedRoadAddresses = ListBuffer[Long]()
    withDynTransaction {
      roadAddressAmount = RoadAddressDAO.getRoadAddressAmmount()
    }
    var stopCondition: Long = 0
    var lastProcessedId: Long = 0
    var endIndex = batchSize
    if (endIndex >= roadAddressAmount){
      endIndex = roadAddressAmount
    }
    while(stopCondition == 0) {
      //Begin the processing loop

      //Fetch the roadAddresses
      println(s"\nProcessing ${endIndex}/${roadAddressAmount} road addresses at time : ${DateTime.now()}")
      var roadAddressList: List[RoadAddress] = List[RoadAddress]()
      withDynTransaction {
        roadAddressList = RoadAddressDAO.getAllRoadAddressesByRange(lastProcessedId, batchSize)
      }

      //Fetch the associated roadLinks
      println(s"\nFetching the road links associated to the road addresses at time : ${DateTime.now()}")
      var roadLinkList: Seq[VVHRoadlink] = Seq[VVHRoadlink]()
      withDynTransaction {
        roadLinkList = roadLinkService.fetchVVHRoadlinks(roadAddressList.map(_.linkId).toSet)
      }

      println(s"\nDeliberating if the road addresses are floating or not at time : ${DateTime.now()}")
      roadAddressList.foreach(ra => {
        try {
          processFloatingRoadAddress(ra, roadLinkList)
          lastProcessedId = ra.id
        } catch {
            case e: Exception => {
              nonProcessedRoadAddresses += ra.id
            }
        }
      })
      //Update the variables for the next loop
      if (endIndex == roadAddressAmount) {
        stopCondition = 1
      } else if(endIndex + batchSize > roadAddressAmount){
        endIndex = roadAddressAmount
      } else {
        endIndex = endIndex + batchSize
      }
    }
    //Check if the there were any road addresses that were not processed, if there are print a warning with the ID's of them.
    val nonProcessedRoadAddressString = nonProcessedRoadAddresses.toList
    if(!nonProcessedRoadAddressString.isEmpty){
      val nonProcessedString = s"\nThe id's of the following road addresses were not processed: "
      nonProcessedRoadAddressString.foreach(ra =>{
        nonProcessedString + ra + ", "
      })
      println(nonProcessedString.stripSuffix(", "))
    }
  }

  /**
    * Main processing part of each road address, deliberates if that road address is a floating one based on the inexistence of roadlinks for that road address linkId.
    *
    * @param selectedRoadAddress The road address to be analyzed
    * @param roadLinkList The list of the roadlinks related to that road address
    */
  private def processFloatingRoadAddress(selectedRoadAddress: RoadAddress, roadLinkList: Seq[VVHRoadlink]) = {
    //Get the roadLinks with the same LinkId
    val roadLinks = roadLinkList.filter(_.linkId == selectedRoadAddress.linkId)
    //If there is no roadLink for the LinkId in the road address then we mark said road address as "floating"
    if (roadLinks.isEmpty) {
      //Mark the road address as floating (FLOATING = 1)
      withDynTransaction {
        RoadAddressDAO.changeRoadAddressFloating(1, selectedRoadAddress.id)
      }
    } else {
      var nonProcessedRoadLinks = ListBuffer[Long]()
      roadLinks.foreach(selectedRoadLink => {
        try {
          processFloatingRoadLinks(selectedRoadAddress, selectedRoadLink)
        } catch {
          case e: Exception => {
            nonProcessedRoadLinks += selectedRoadLink.linkId
          }
        }

      })

      //Check if the there were any road links that were not processed, if there are print a warning with the ID's of them.
      val nonProcessedRoadLinksString = nonProcessedRoadLinks.toList
      if(!nonProcessedRoadLinksString.isEmpty){
        val nonProcessedString = s"\nThe LinkId's of the following road links related to the road address: ${selectedRoadAddress.id} were not processed: "
        nonProcessedRoadLinksString.foreach(ra =>{
          nonProcessedString + ra + ", "
        })
        println(nonProcessedString.stripSuffix(", "))
      }

    }
  }

  /**
    * Main processing part for the road links related to a road address, the relationship between the two
    * is formed by validating that the LinkId of the road address is the same for the Link Id of the road link.
    * This rules is to be processed only if there are road links for the road address.
    *
    * @param selectedRoadAddress The selected road address
    * @param selectedRoadLink The selected road link
    */
  private def processFloatingRoadLinks(selectedRoadAddress: RoadAddress, selectedRoadLink: VVHRoadlink ) ={
    //If there is we need to check if the length of the roadlink is < than the startM value of the roadAddress, if it is not
    if (selectedRoadAddress.startMValue > selectedRoadLink.geometry.size) {
      //Mark the road address as floating (FLOATING = 1)
      withDynTransaction {
        RoadAddressDAO.changeRoadAddressFloating(1, selectedRoadAddress.id)
      }
    } else {
      //Mark the road address as not floating (FLOATING = 0)
      withDynTransaction {
        RoadAddressDAO.changeRoadAddressFloating(0, selectedRoadAddress.id)
      }
    }
  }

}



object RoadAddressLinkBuilder {
  val RoadNumber = "ROADNUMBER"
  val RoadPartNumber = "ROADPARTNUMBER"

  val formatter = DateTimeFormat.forPattern("dd.MM.yyyy")

  def build(roadLink: RoadLink, roadAddress: RoadAddress) = {
    val geom = GeometryUtils.truncateGeometry2D(roadLink.geometry, roadAddress.startMValue, roadAddress.endMValue)
    val length = GeometryUtils.geometryLength(geom)
    new RoadAddressLink(roadAddress.id, roadLink.linkId, geom,
      length, roadLink.administrativeClass,
      roadLink.functionalClass, roadLink.trafficDirection,
      roadLink.linkType, roadLink.modifiedAt, roadLink.modifiedBy,
      roadLink.attributes, roadAddress.roadNumber, roadAddress.roadPartNumber, roadAddress.track.value, roadAddress.ely, roadAddress.discontinuity.value,
      roadAddress.startAddrMValue, roadAddress.endAddrMValue, formatter.print(roadAddress.endDate), roadAddress.startMValue, roadAddress.endMValue,
      toSideCode(roadAddress.startMValue, roadAddress.endMValue, roadAddress.track),
      roadAddress.calibrationPoints._1,
      roadAddress.calibrationPoints._2)

  }
  def build(roadLink: RoadLink, missingAddress: MissingRoadAddress) = {
    val geom = GeometryUtils.truncateGeometry2D(roadLink.geometry, missingAddress.startMValue.getOrElse(0.0), missingAddress.endMValue.getOrElse(roadLink.length))
    val length = GeometryUtils.geometryLength(geom)
    val roadLinkRoadNumber = roadLink.attributes.get(RoadNumber).map(toIntNumber).getOrElse(0)
    val roadLinkRoadPartNumber = roadLink.attributes.get(RoadPartNumber).map(toIntNumber).getOrElse(0)
    new RoadAddressLink(0, roadLink.linkId, geom,
      length, roadLink.administrativeClass,
      roadLink.functionalClass, roadLink.trafficDirection,
      roadLink.linkType, roadLink.modifiedAt, roadLink.modifiedBy,
      roadLink.attributes, missingAddress.roadNumber.getOrElse(roadLinkRoadNumber),
      missingAddress.roadPartNumber.getOrElse(roadLinkRoadPartNumber), Track.Unknown.value, 0, Discontinuity.Continuous.value,
      0, 0, "", 0.0, length, SideCode.Unknown,
      None,
      None, missingAddress.anomaly)
  }

  private def toSideCode(startMValue: Double, endMValue: Double, track: Track) = {
    track match {
      case Track.Combined => SideCode.BothDirections
      case Track.LeftSide => if (startMValue < endMValue) {
        SideCode.TowardsDigitizing
      } else {
        SideCode.AgainstDigitizing
      }
      case Track.RightSide => if (startMValue > endMValue) {
        SideCode.TowardsDigitizing
      } else {
        SideCode.AgainstDigitizing
      }
      case _ => SideCode.Unknown
    }
  }

  private def toIntNumber(value: Any) = {
    try {
      value.asInstanceOf[String].toInt
    } catch {
      case e: Exception => 0
    }
  }

}
