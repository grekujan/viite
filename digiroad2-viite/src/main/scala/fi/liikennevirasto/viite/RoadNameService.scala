package fi.liikennevirasto.viite

import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.user.User
import fi.liikennevirasto.viite.dao.{ProjectLinkNameDAO, RoadName, RoadNameDAO}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal
import org.joda.time.format.DateTimeFormat

case class RoadNameRow(id: Long, name: String, startDate: String, endDate: Option[String])

class RoadNameService() {

  def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)

  def withDynSession[T](f: => T): T = OracleDatabase.withDynSession(f)

  private val logger = LoggerFactory.getLogger(getClass)

  val formatter = DateTimeFormat.forPattern("dd.MM.yyyy")

  def getRoadNames(oRoadNumber: Option[String], oRoadName: Option[String], oStartDate: Option[DateTime], oEndDate: Option[DateTime]): Either[String, Seq[RoadName]] = {
    withDynTransaction {
      getRoadNamesInTX(oRoadNumber, oRoadName, oStartDate, oEndDate)
    }
  }

  def addOrUpdateRoadNames(roadNumber: Long, roadNameRows: Seq[RoadNameRow], user: User): Option[String] = {
    withDynTransaction {
      addOrUpdateRoadNamesInTX(roadNumber, roadNameRows, user)
    }
  }

  def addOrUpdateRoadNamesInTX(roadNumber: Long, roadNameRows: Seq[RoadNameRow], user: User): Option[String] = {
    try {
      roadNameRows.map{
        roadNameRow =>
          val roadNameOption = if(roadNameRow.id == NewRoadNameId) None else RoadNameDAO.getRoadNamesById(roadNameRow.id)
          val endDate = roadNameRow.endDate match {
            case Some(dt) => Some(new DateTime(formatter.parseDateTime(dt)))
            case _ => None
          }
          val newRoadName = roadNameOption match {
            case Some(roadName) =>
              RoadNameDAO.expire(roadNameRow.id, user)
              roadName.copy(createdBy = user.username, roadName = roadNameRow.name, endDate = endDate)
            case _ =>
              val startDate = new DateTime(formatter.parseDateTime(roadNameRow.startDate))
              RoadName(NewRoadNameId, roadNumber, roadNameRow.name, Some(startDate), endDate, createdBy = user.username)
          }

          RoadNameDAO.create(newRoadName)
      }
      None
    } catch {
      case e: Exception => Some(e.getMessage)
      case e: RoadNameException => Some(e.getMessage)
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
  def getRoadNamesInTX(oRoadNumber: Option[String], oRoadName: Option[String], oStartDate: Option[DateTime], oEndDate: Option[DateTime]): Either[String, Seq[RoadName]] = {
    try {
      (oRoadNumber, oRoadName) match {
        case (Some(roadNumber), Some(roadName)) =>
          Right(RoadNameDAO.getAllByRoadNumberAndName(roadNumber.toLong, roadName, oStartDate, oEndDate))
        case (None, Some(roadName)) =>
          Right(RoadNameDAO.getAllByRoadName(roadName, oStartDate, oEndDate))
        case (Some(roadNumber), None) =>
          Right(RoadNameDAO.getAllByRoadNumber(roadNumber.toLong, oStartDate, oEndDate))
        case (None, None) => Left("Missing RoadNumber")
      }
    } catch {
      case longParsingException: NumberFormatException => Left("Could not parse road number")
      case e if NonFatal(e) => Left("Unknown error"+e)
    }
  }

  /**
    * Fetches road names that are updated after the given date.
    *
    * @param since
    * @return Returns error message as left and seq of road names as right
    */
  def getUpdatedRoadNames(since: DateTime): Either[String, Seq[RoadName]] = {
    withDynTransaction {
      getUpdatedRoadNamesInTX(since)
    }
  }

  def getUpdatedRoadNamesInTX(since: DateTime): Either[String, Seq[RoadName]] = {
    try {
      Right(RoadNameDAO.getUpdatedRoadNames(since))
    } catch {
      case e if NonFatal(e) =>
        logger.error("Failed to fetch updated road names.", e)
        Left(e.getMessage)
    }
  }

  def getRoadNameByNumber(roadNumber: Long, projectID: Long): Option[Map[String, Any]] = {
    try {
      withDynSession {
        val currentRoadNames = RoadNameDAO.getCurrentRoadNamesByRoadNumber(roadNumber)
        if (currentRoadNames.isEmpty) {
          val projectRoadNames = ProjectLinkNameDAO.get(roadNumber, projectID)
          if (projectRoadNames.isEmpty) {
            return None
          }
          else {
            Some(Map("roadName" -> projectRoadNames.get.roadName, "isCurrent" -> false))
          }
        }
        else
          Some(Map("roadName" -> currentRoadNames.head.roadName, "isCurrent" -> true))
      }
    }
    catch {
      case longParsingException: NumberFormatException => Some(Map("error" -> "Could not parse road number"))
      case e if NonFatal(e) => Some(Map("error" -> "Unknown error"))
    }
  }

  def getHasCurrentRoadName(roadNumber: Long): Boolean = {
    withDynSession {
      RoadNameDAO.getCurrentRoadNamesByRoadNumber(roadNumber).nonEmpty
    }
  }

}

class RoadNameException(string: String) extends RuntimeException {
  override def getMessage: String = string
}
