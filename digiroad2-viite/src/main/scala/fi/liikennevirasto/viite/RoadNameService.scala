package fi.liikennevirasto.viite

import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.user.User
import fi.liikennevirasto.viite.dao.{RoadName, RoadNameDAO}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import scala.util.control.NonFatal

case class RoadNameRows(roadId: Long, editions: Seq[RoadNameEditions])

case class RoadNameEditions(editedField: String, value: String)

class RoadNameService() {

  def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)

  def withDynSession[T](f: => T): T = OracleDatabase.withDynSession(f)

  private val logger = LoggerFactory.getLogger(getClass)

  private def queryRoadNamesAndNumbers(oRoadNumber: Option[Long], oRoadName: Option[String],
                                       oStartDate: Option[DateTime] = None, oEndDate: Option[DateTime] = None): Seq[RoadName] = {
    RoadNameDAO.getRoadNamesByRoadNameAndRoadNumber(oRoadNumber, oRoadName, None, None, oStartDate, oEndDate)
  }

  import org.joda.time.DateTime
  import org.joda.time.format.DateTimeFormat

  val formatter = DateTimeFormat.forPattern("dd.MM.yyyy")

  private def singleNameExpiration(nameRows: Seq[RoadNameRows]) = {
    val field = decodeFields(nameRows.head.editions)
    val toBeExpired = nameRows.size == 1 && nameRows.head.roadId != NewRoadName && field.size == 1 && field.head._1 == "roadName"
    (toBeExpired, field.head._2)
  }

  def getRoadAddressesInTx(oRoadNumber: Option[String], oRoadName: Option[String], oStartDate: Option[DateTime], oEndDate: Option[DateTime]): Either[String, Seq[RoadName]] = {
    withDynTransaction {
      getRoadAddresses(oRoadNumber, oRoadName, oStartDate, oEndDate)
    }
  }

  def addOrUpdateRoadNamesInTx(roadNames: Seq[RoadNameRows], user: User, newTransaction: Boolean = true): Option[String] = {
    if(newTransaction)
    withDynTransaction {
      addOrUpdateRoadNames(roadNames, user)
    } else
      addOrUpdateRoadNames(roadNames, user)
    }

  def addOrUpdateRoadNames(roadNames: Seq[RoadNameRows], user: User): Option[String] = {
    try {
      val (isRoadNameExpiration, newName) = singleNameExpiration(roadNames)
      if(isRoadNameExpiration){
        val road = RoadNameDAO.getRoadNamesById(roadNames.head.roadId)
        RoadNameDAO.expire(roadNames.head.roadId, user)
        RoadNameDAO.create(road.copy(createdBy = user.username, roadName = newName))
      } else {
      roadNames.foreach(rn => {
        val fieldMaps = decodeFields(rn.editions)
        if (rn.roadId == NewRoadName) {
          val roadNumber = fieldMaps.get("roadNumber")
          val roadName = fieldMaps.get("roadName")
          val startDate = fieldMaps.get("startDate") match {
            case Some(dt) => Some(new DateTime(formatter.parseDateTime(dt)))
            case _ => None
          }
          val endDate = fieldMaps.get("endDate") match {
            case Some(dt) => Some(new DateTime(formatter.parseDateTime(dt)))
            case _ => None
          }
          val road = RoadName(rn.roadId, roadNumber.get.toLong, roadName.get, startDate, endDate, createdBy = user.username)
          RoadNameDAO.create(road)
        } else {
          RoadNameDAO.update(rn.roadId, fieldMaps, user)
        }
      })
      }
      None
    } catch {
      case e: Exception => Some(e.getMessage)
      case e: RoadNameException => Some(e.getMessage)
    }
  }

  private def decodeFields(editions: Seq[RoadNameEditions]) = {
    editions.foldLeft(Map.empty[String, String]) { (map, edit) =>
      CombineMaps.combine(map, Map(edit.editedField -> edit.value))
    }
  }

  /**
    * Searches road names by road number, road name and between history
    *
    * @param oRoadNumber Option road number
    * @param oRoadName   Option road name
    * @param oStartDate  Option start date
    * @param oEndDate    Option end date
    * @return Returns error message as left and right as seq of road names
    */
  def getRoadAddresses(oRoadNumber: Option[String], oRoadName: Option[String], oStartDate: Option[DateTime], oEndDate: Option[DateTime]): Either[String, Seq[RoadName]] = {
    try {
      (oRoadNumber, oRoadName) match {
        case (Some(roadNumber), Some(roadName)) =>
          Right(queryRoadNamesAndNumbers(Some(roadNumber.toLong), Some(roadName), oStartDate, oEndDate))
        case (None, Some(roadName)) =>
          Right(queryRoadNamesAndNumbers(None, Some(roadName), oStartDate, oEndDate))
        case (Some(roadNumber), None) =>
          Right(RoadNameDAO.getRoadNamesByRoadNumber(roadNumber.toLong, None, None, oStartDate, oEndDate))
        case (None, None) => Left("Missing RoadNumber")
      }
    } catch {
      case longParsingException: NumberFormatException => Left("Could not parse road number")
      case e if NonFatal(e) => Left("Unknown error")
    }
  }


  def getRoadNameByNumber(roadNumber: Long) : String= {
    try{
      withDynSession{
        val roadNames = RoadNameDAO.getCurrentRoadNamesByRoadNumber(roadNumber)
        if(roadNames.isEmpty)
          ""
        else
          roadNames.head.roadName
      }
    }
    catch {
      case longParsingException: NumberFormatException => "Could not parse road number"
      case e if NonFatal(e) => "Unknown error"
    }
  }
}

  class RoadNameException(string: String) extends RuntimeException {
    override def getMessage: String = string
  }
