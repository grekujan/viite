package fi.liikennevirasto.viite

import fi.liikennevirasto.digiroad2.asset.SideCode.{AgainstDigitizing, TowardsDigitizing}
import fi.liikennevirasto.digiroad2.asset.{LinkGeomSource, SideCode}
import fi.liikennevirasto.digiroad2.dao.Sequences
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase
import fi.liikennevirasto.digiroad2.util.Track
import fi.liikennevirasto.digiroad2.util.Track.{Combined, LeftSide, RightSide}
import fi.liikennevirasto.digiroad2.{Point, Vector3d}
import fi.liikennevirasto.viite.ProjectValidator.ValidationErrorList._
import fi.liikennevirasto.viite.dao.Discontinuity.EndOfRoad
import fi.liikennevirasto.viite.dao.TerminationCode.NoTermination
import fi.liikennevirasto.viite.dao.{TerminationCode, _}
import org.joda.time.DateTime
import org.scalatest.{FunSuite, Matchers}
import slick.driver.JdbcDriver.backend.Database
import slick.driver.JdbcDriver.backend.Database.dynamicSession

class ProjectValidatorSpec extends FunSuite with Matchers {
  def withDynTransaction[T](f: => T): T = OracleDatabase.withDynTransaction(f)

  def runWithRollback[T](f: => T): T = {
    Database.forDataSource(OracleDatabase.ds).withDynTransaction {
      val t = f
      dynamicSession.rollback()
      t
    }
  }

  private def testDataForCheckTerminationContinuity(noErrorTest: Boolean = false) = {
    val roadAddressId = RoadAddressDAO.getNextRoadAddressId
    val ra = Seq(RoadAddress(roadAddressId, 27L, 20L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous, 6109L, 6559L,
      Some(DateTime.parse("1996-01-01")), None, Option("TR"), 0, 1817196, 0.0, 108.261, SideCode.AgainstDigitizing, 1476392565000L, (None, None), floating = false,
      Seq(Point(0.0, 40.0), Point(0.0, 50.0)), LinkGeomSource.NormalLinkInterface, 8, TerminationCode.NoTermination, 0))
    if(noErrorTest) {
      val roadsToCreate = ra ++ Seq(RoadAddress(RoadAddressDAO.getNextRoadAddressId, 26L, 21L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous, 6559L, 5397L,
        Some(DateTime.parse("1996-01-01")), None, Option("TR"), 1, 1817197, 0.0, 108.261, SideCode.AgainstDigitizing, 1476392565000L, (None, None), floating = false,
        Seq(Point(0.0, 40.0), Point(0.0, 55.0)), LinkGeomSource.NormalLinkInterface, 8, TerminationCode.NoTermination, 0),
        RoadAddress(RoadAddressDAO.getNextRoadAddressId, 27L, 22L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous, 6559L, 5397L,
          Some(DateTime.parse("1996-01-01")), None, Option("TR"), 1, 1817198, 0.0, 108.261, SideCode.AgainstDigitizing, 1476392565000L, (None, None), floating = false,
          Seq(Point(0.0, 40.0), Point(0.0, 55.0)), LinkGeomSource.NormalLinkInterface, 8, TerminationCode.NoTermination, 0),
        RoadAddress(RoadAddressDAO.getNextRoadAddressId, 27L, 23L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous, 6559L, 5397L,
          Some(DateTime.parse("1996-01-01")), None, Option("TR"), 1, 1817199, 0.0, 108.261, SideCode.AgainstDigitizing, 1476392565000L, (None, None), floating = false,
          Seq(Point(0.0, 120.0), Point(0.0, 130.0)), LinkGeomSource.NormalLinkInterface, 8, TerminationCode.NoTermination, 0))
      RoadAddressDAO.create(roadsToCreate)
    } else {
      RoadAddressDAO.create(ra)
    }
  }

  private def testDataForElyTest01() = {
    val roadAddressId = RoadAddressDAO.getNextRoadAddressId
    val ra = Seq(RoadAddress(roadAddressId, 16320L, 2L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous, 1270L, 1309L,
      Some(DateTime.parse("1982-09-01")), None, Option("TR"), 0, 2583382, 0.0, 38.517, SideCode.AgainstDigitizing, 1476392565000L, (None, None), floating = false,
      Seq(Point(0.0, 40.0), Point(0.0, 50.0)), LinkGeomSource.NormalLinkInterface, 8, TerminationCode.NoTermination, 0))
    RoadAddressDAO.create(ra)

  }

  private def testDataForElyTest02() = {
    val roadAddressId = RoadAddressDAO.getNextRoadAddressId
    val ra = Seq(RoadAddress(roadAddressId, 27L, 20L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous, 6109L, 6559L,
      Some(DateTime.parse("1996-01-01")), None, Option("TR"), 0, 1817196, 0.0, 108.261, SideCode.AgainstDigitizing, 1476392565000L, (None, None), floating = false,
      Seq(Point(0.0, 40.0), Point(0.0, 50.0)), LinkGeomSource.NormalLinkInterface, 8, TerminationCode.NoTermination, 0))
    RoadAddressDAO.create(ra)
  }

  test("Project Links should be continuous if geometry is continuous") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L))
      val endOfRoadSet = projectLinks.init :+ projectLinks.last.copy(discontinuity = EndOfRoad)
      ProjectValidator.checkOrdinaryRoadContinuityCodes(project, endOfRoadSet) should have size 0
      val brokenContinuity = endOfRoadSet.tail :+ endOfRoadSet.head.copy(geometry = projectLinks.head.geometry.map(_ + Vector3d(1.0, 1.0, 0.0)))
      val errors = ProjectValidator.checkOrdinaryRoadContinuityCodes(project, brokenContinuity)
      errors should have size 1
      errors.head.validationError should be(MinorDiscontinuityFound)
    }
  }

  test("Project Links missing end of road should be caught") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L))
      val errors = ProjectValidator.checkOrdinaryRoadContinuityCodes(project, projectLinks)
      errors should have size 1
      errors.head.validationError should be(MissingEndOfRoad)
    }
  }

  test("Project Links must not have an end of road code if next part exists in project") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L))
      ProjectDAO.reserveRoadPart(project.id, 19999L, 2L, "u")
      ProjectDAO.create(projectLinks.map(l => l.copy(id = NewRoadAddress, roadPartNumber = 2L, createdBy = Some("User"),
        geometry = l.geometry.map(_ + Vector3d(0.0, 40.0, 0.0)))))
      val updProject = ProjectDAO.getRoadAddressProjectById(project.id).get
      val errors = ProjectValidator.checkOrdinaryRoadContinuityCodes(updProject, projectLinks)
      ProjectDAO.getProjectLinks(project.id) should have size 8
      errors should have size 0
      val (starting, last) = projectLinks.splitAt(3)
      val errorsUpd = ProjectValidator.checkOrdinaryRoadContinuityCodes(updProject,
        starting ++ last.map(_.copy(discontinuity = EndOfRoad)))
      errorsUpd should have size 1
      errorsUpd.head.validationError should be(EndOfRoadNotOnLastPart)
    }
  }

  test("Project Links must not have an end of road code if next part exists in road address table") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L))
      ProjectValidator.checkOrdinaryRoadContinuityCodes(project, projectLinks) should have size 1
      RoadAddressDAO.create(Seq(RoadAddress(NewRoadAddress, 19999L, 2L, RoadType.PublicRoad, Track.Combined, Discontinuity.EndOfRoad,
        0L, 10L, Some(DateTime.now()), None, None, 0L, 39399L, 0.0, 10.0, TowardsDigitizing, 0L, (Some(CalibrationPoint(39399L, 0.0, 0L)), Some(CalibrationPoint(39399L, 10.0, 10L))),
        floating = false, Seq(Point(0.0, 40.0), Point(0.0, 50.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0)))
      val errors = ProjectValidator.checkOrdinaryRoadContinuityCodes(project, projectLinks)
      errors should have size 0
      val (starting, last) = projectLinks.splitAt(3)
      val errorsUpd = ProjectValidator.checkOrdinaryRoadContinuityCodes(project,
        starting ++ last.map(_.copy(discontinuity = EndOfRoad)))
      errorsUpd should have size 1
      errorsUpd.head.validationError should be(EndOfRoadNotOnLastPart)
    }
  }

  test("Project Links must have a major discontinuity code if and only if next part exists in road address / project link table and is not connected") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L))
      val raId = RoadAddressDAO.create(Seq(RoadAddress(NewRoadAddress, 19999L, 2L, RoadType.PublicRoad, Track.Combined, Discontinuity.EndOfRoad,
        0L, 10L, Some(DateTime.now()), None, None, 0L, 39399L, 0.0, 10.0, TowardsDigitizing, 0L, (Some(CalibrationPoint(39399L, 0.0, 0L)), Some(CalibrationPoint(39399L, 10.0, 10L))),
        floating = false, Seq(Point(10.0, 40.0), Point(10.0, 50.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0))).head
      val errors = ProjectValidator.checkOrdinaryRoadContinuityCodes(project, projectLinks)
      errors should have size 1
      errors.head.validationError should be(MajorDiscontinuityFound)

      val (starting, last) = projectLinks.splitAt(3)
      val errorsUpd = ProjectValidator.checkOrdinaryRoadContinuityCodes(project,
        starting ++ last.map(_.copy(discontinuity = Discontinuity.Discontinuous)))
      errorsUpd should have size 0

      RoadAddressDAO.updateGeometry(raId, Seq(Point(0.0, 40.0), Point(0.0, 50.0)))

      val connectedError = ProjectValidator.checkOrdinaryRoadContinuityCodes(project,
        starting ++ last.map(_.copy(discontinuity = Discontinuity.Discontinuous)))
      connectedError should have size 1
      connectedError.head.validationError should be(ConnectedDiscontinuousLink)
    }
  }
  //TODO to be done/changed in a more detailed story
  ignore("Project Links must have a ely change discontinuity code if next part is on different ely") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L))
      val raId = RoadAddressDAO.create(Seq(RoadAddress(NewRoadAddress, 19999L, 2L, RoadType.PublicRoad, Track.Combined, Discontinuity.EndOfRoad,
        0L, 10L, Some(DateTime.now()), None, None, 0L, 39399L, 0.0, 10.0, TowardsDigitizing, 0L, (Some(CalibrationPoint(39399L, 0.0, 0L)), Some(CalibrationPoint(39399L, 10.0, 10L))),
        floating = false, Seq(Point(10.0, 40.0), Point(10.0, 50.0)), LinkGeomSource.ComplimentaryLinkInterface, 9L, NoTermination, 0))).head
      val errors = ProjectValidator.checkOrdinaryRoadContinuityCodes(project, projectLinks)
      errors should have size 1
      errors.head.validationError should be(ElyCodeChangeDetected)

      val (starting, last) = projectLinks.splitAt(3)
      val errorsUpd = ProjectValidator.checkOrdinaryRoadContinuityCodes(project,
        starting ++ last.map(_.copy(discontinuity = Discontinuity.ChangingELYCode)))
      errorsUpd should have size 0

      RoadAddressDAO.updateGeometry(raId, Seq(Point(0.0, 40.0), Point(0.0, 50.0)))

      val connectedError = ProjectValidator.checkOrdinaryRoadContinuityCodes(project,
        starting ++ last.map(_.copy(discontinuity = Discontinuity.Continuous)))
      connectedError should have size 1
      connectedError.head.validationError should be(ElyCodeChangeDetected)
    }
  }

  test("Check end of road after terminations in project with multiple parts (checkRemovedEndOfRoadParts method)") {
    //Now this validation returns 0 errors, because the previous road part is also reserved on the same project, and the error should not be TerminationContinuity, but MissingEndOfRoad
    //and that is not checked on checkRemovedEndOfRoadParts method
    runWithRollback {
      val ra = Seq(
        RoadAddress(NewRoadAddress, 19999L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous,
          0L, 10L, Some(DateTime.now()), None, None, 0L, 39398L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39398L, 0.0, 0L)), Some(CalibrationPoint(39398L, 10.0, 10L))),
          floating = false, Seq(Point(10.0, 30.0), Point(10.0, 40.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0),
        RoadAddress(NewRoadAddress, 19999L, 2L, RoadType.PublicRoad, Track.Combined, Discontinuity.EndOfRoad,
          0L, 10L, Some(DateTime.now()), None, None, 0L, 39399L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39399L, 0.0, 0L)), Some(CalibrationPoint(39399L, 10.0, 10L))),
          floating = false, Seq(Point(10.0, 40.0), Point(10.0, 50.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0))
      val raIds = RoadAddressDAO.create(ra, Some("U"))
      val roadAddress = RoadAddressDAO.fetchByIdMassQuery(raIds.toSet).sortBy(_.roadPartNumber)
      val id = Sequences.nextViitePrimaryKeySeqValue
      val project = RoadAddressProject(id, ProjectState.Incomplete, "f", "s", DateTime.now(), "", DateTime.now(), DateTime.now(),
        "", Seq(), None, Some(8), None)
      ProjectDAO.createRoadAddressProject(project)
      ProjectDAO.reserveRoadPart(id, 19999L, 1L, "u")
      ProjectDAO.reserveRoadPart(id, 19999L, 2L, "u")

      ProjectDAO.create(Seq(util.projectLink(0L, 10L, Combined, id, LinkStatus.UnChanged),
        util.projectLink(0L, 10L, Combined, id, LinkStatus.Terminated)).zip(roadAddress).map(x => x._1.copy(roadPartNumber = x._2.roadPartNumber,
        roadAddressId = x._2.id, geometry = x._2.geometry, discontinuity = x._2.discontinuity)))
      val updProject = ProjectDAO.getRoadAddressProjectById(project.id).get
      val errors = ProjectValidator.checkRemovedEndOfRoadParts(updProject)
      errors should have size 0
    }
  }

  test("Check end of road after terminations in project with single parts (checkRemovedEndOfRoadParts method)") {
    runWithRollback {
      val ra = Seq(
        RoadAddress(NewRoadAddress, 19999L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous,
          0L, 10L, Some(DateTime.now()), None, None, 0L, 39398L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39398L, 0.0, 0L)), Some(CalibrationPoint(39398L, 10.0, 10L))),
          floating = false, Seq(Point(10.0, 30.0), Point(10.0, 40.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0),
        RoadAddress(NewRoadAddress, 19999L, 2L, RoadType.PublicRoad, Track.Combined, Discontinuity.EndOfRoad,
          0L, 10L, Some(DateTime.now()), None, None, 0L, 39399L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39399L, 0.0, 0L)), Some(CalibrationPoint(39399L, 10.0, 10L))),
          floating = false, Seq(Point(10.0, 40.0), Point(10.0, 50.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0))
      val raIds = RoadAddressDAO.create(ra, Some("U"))
      val roadAddress = RoadAddressDAO.fetchByIdMassQuery(raIds.toSet).maxBy(_.roadPartNumber)
      val id = Sequences.nextViitePrimaryKeySeqValue
      val project = RoadAddressProject(id, ProjectState.Incomplete, "f", "s", DateTime.now(), "", DateTime.now(), DateTime.now(),
        "", Seq(), None, Some(8), None)
      ProjectDAO.createRoadAddressProject(project)
      ProjectDAO.reserveRoadPart(id, 19999L, 2L, "u")

      ProjectDAO.create(Seq(util.projectLink(0L, 10L, Combined, id, LinkStatus.Terminated)).zip(Seq(roadAddress)).map(x => x._1.copy(roadPartNumber = x._2.roadPartNumber,
        roadAddressId = x._2.id, geometry = x._2.geometry, discontinuity = x._2.discontinuity)))
      val updProject = ProjectDAO.getRoadAddressProjectById(project.id).get
      val errors = ProjectValidator.checkRemovedEndOfRoadParts(updProject)
      errors should have size 1
      errors.head.validationError.value should be(TerminationContinuity.value)
      errors.head.validationError.message should be("Tekemäsi tieosoitemuutoksen vuoksi projektin ulkopuoliselle tieosalle täytyy muuttaa jatkuvuuskoodi Tien loppu. Muuta jatkuvuuskoodiksi Tien loppu (1) tieosoitteelle: (19999,1).")
      val projectLinks = ProjectDAO.getProjectLinks(id, Some(LinkStatus.Terminated)).map(_.copy(discontinuity = EndOfRoad, status = LinkStatus.UnChanged))
      ProjectDAO.updateProjectLinksToDB(projectLinks, "U")
      val updProject2 = ProjectDAO.getRoadAddressProjectById(project.id).get
      ProjectValidator.checkRemovedEndOfRoadParts(updProject2) should have size 0
    }
  }

  test("Ramps must have continuity validation") {
    runWithRollback {
      val project = util.setUpProjectWithRampLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L))
      val projectLinks = ProjectDAO.getProjectLinks(project.id)
      val errors = ProjectValidator.checkRampContinuityCodes(project, projectLinks)
      errors should have size 0

      val (starting, last) = projectLinks.splitAt(3)
      val errorsUpd = ProjectValidator.checkRampContinuityCodes(project,
        starting ++ last.map(_.copy(discontinuity = Discontinuity.Continuous)))
      errorsUpd should have size 1

      val errorsUpd2 = ProjectValidator.checkRampContinuityCodes(project,
        starting ++ last.map(_.copy(discontinuity = Discontinuity.MinorDiscontinuity)))
      errorsUpd2 should have size 1

      val ra = Seq(
        RoadAddress(NewRoadAddress, 39998L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous,
          0L, 10L, Some(DateTime.now()), None, None, 0L, 39398L, 0.0, 10.0, AgainstDigitizing, 0L,
          (Some(CalibrationPoint(39398L, 0.0, 0L)), Some(CalibrationPoint(39398L, 10.0, 10L))),
          floating = false, Seq(Point(2.0, 30.0), Point(0.0, 40.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0),
        RoadAddress(NewRoadAddress, 39998L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous,
          10L, 20L, Some(DateTime.now()), None, None, 0L, 39398L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39398L, 0.0, 0L)), Some(CalibrationPoint(39398L, 10.0, 10L))),
          floating = false, Seq(Point(2.0, 30.0), Point(7.0, 35.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0),
        RoadAddress(NewRoadAddress, 39998L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.EndOfRoad,
          20L, 30L, Some(DateTime.now()), None, None, 0L, 39399L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39399L, 0.0, 0L)), Some(CalibrationPoint(39399L, 10.0, 10L))),
          floating = false, Seq(Point(7.0, 35.0), Point(0.0, 40.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0))
      RoadAddressDAO.create(ra)

      ProjectDAO.reserveRoadPart(project.id, 39999L, 20L, "u")
      ProjectDAO.create((starting ++ last.map(_.copy(discontinuity = Discontinuity.EndOfRoad)))
        .map(_.copy(id = NewRoadAddress, roadPartNumber = 20L, createdBy = Some("I"))))
      val updProject = ProjectDAO.getRoadAddressProjectById(project.id).get
      ProjectValidator.checkRampContinuityCodes(updProject,
        starting ++ last.map(_.copy(discontinuity = Discontinuity.MinorDiscontinuity))) should have size 0
    }
  }

  test("validator should produce an error on Not Handled links") {
    runWithRollback {
      val ra = Seq(
        RoadAddress(NewRoadAddress, 19999L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous,
          0L, 10L, Some(DateTime.now()), None, None, 0L, 39398L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39398L, 0.0, 0L)), Some(CalibrationPoint(39398L, 10.0, 10L))),
          floating = false, Seq(Point(10.0, 30.0), Point(10.0, 40.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0),
        RoadAddress(NewRoadAddress, 19999L, 2L, RoadType.PublicRoad, Track.Combined, Discontinuity.EndOfRoad,
          0L, 10L, Some(DateTime.now()), None, None, 0L, 39399L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39399L, 0.0, 0L)), Some(CalibrationPoint(39399L, 10.0, 10L))),
          floating = false, Seq(Point(10.0, 40.0), Point(10.0, 50.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0))
      val raIds = RoadAddressDAO.create(ra, Some("U"))
      val roadAddress = RoadAddressDAO.fetchByIdMassQuery(raIds.toSet).sortBy(_.roadPartNumber)
      val id = Sequences.nextViitePrimaryKeySeqValue
      val project = RoadAddressProject(id, ProjectState.Incomplete, "f", "s", DateTime.now(), "", DateTime.now(), DateTime.now(),
        "", Seq(), None, Some(8), None)
      ProjectDAO.createRoadAddressProject(project)
      ProjectDAO.reserveRoadPart(id, 19999L, 1L, "u")
      ProjectDAO.reserveRoadPart(id, 19999L, 2L, "u")

      ProjectDAO.create(Seq(util.projectLink(0L, 10L, Combined, id, LinkStatus.NotHandled),
        util.projectLink(0L, 10L, Combined, id, LinkStatus.Terminated)).zip(roadAddress).map(x => x._1.copy(roadPartNumber = x._2.roadPartNumber,
        roadAddressId = x._2.id, geometry = x._2.geometry, discontinuity = x._2.discontinuity)))

      val validationErrors = ProjectValidator.validateProject(project, ProjectDAO.getProjectLinks(project.id)).filter(_.validationError.value == HasNotHandledLinks.value)
      validationErrors.size should be(1)
      validationErrors.head.validationError.message should be("")
      validationErrors.head.optionalInformation should not be ("")
    }
  }

  test("validator should return invalid unchanged links error") {
    runWithRollback {
      val ra = Seq(
        RoadAddress(NewRoadAddress, 19999L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous,
          0L, 10L, Some(DateTime.now()), None, None, 0L, 39398L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39398L, 0.0, 0L)), Some(CalibrationPoint(39398L, 10.0, 10L))),
          floating = false, Seq(Point(10.0, 30.0), Point(10.0, 40.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0),
        RoadAddress(NewRoadAddress, 19999L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.EndOfRoad,
          10L, 20L, Some(DateTime.now()), None, None, 0L, 39399L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39399L, 0.0, 0L)), Some(CalibrationPoint(39399L, 10.0, 10L))),
          floating = false, Seq(Point(10.0, 40.0), Point(10.0, 50.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0))
      val raId1 = RoadAddressDAO.create(Set(ra.head), Some("U"))
      val raId2 = RoadAddressDAO.create(ra.tail, Some("U"))
      val roadAddress1 = RoadAddressDAO.fetchByIdMassQuery(raId1.toSet).sortBy(_.roadPartNumber)
      val roadAddress2 = RoadAddressDAO.fetchByIdMassQuery(raId2.toSet).sortBy(_.roadPartNumber)
      val id = Sequences.nextViitePrimaryKeySeqValue
      val project = RoadAddressProject(id, ProjectState.Incomplete, "f", "s", DateTime.now(), "", DateTime.now(), DateTime.now(),
        "", Seq(), None, Some(8), None)
      ProjectDAO.createRoadAddressProject(project)
      ProjectDAO.reserveRoadPart(id, 19999L, 1L, "u")

      ProjectDAO.create(Seq(util.projectLink(0L, 10L, Combined, id, LinkStatus.NotHandled),
        util.projectLink(0L, 10L, Combined, id, LinkStatus.Transfer)).zip(roadAddress1).map(x => x._1.copy(roadPartNumber = x._2.roadPartNumber,
        roadAddressId = x._2.id, geometry = x._2.geometry, discontinuity = x._2.discontinuity)))
      ProjectDAO.create(Seq(util.projectLink(10L, 20L, Combined, id, LinkStatus.NotHandled),
        util.projectLink(10L, 20L, Combined, id, LinkStatus.UnChanged)).zip(roadAddress2).map(x => x._1.copy(roadPartNumber = x._2.roadPartNumber,
        roadAddressId = x._2.id, geometry = x._2.geometry, discontinuity = x._2.discontinuity)))

      val projectLinks = ProjectDAO.getProjectLinks(id, Some(LinkStatus.NotHandled))
      val updatedProjectLinks = Seq(projectLinks.head.copy(status = LinkStatus.Transfer)) ++ projectLinks.tail.map(pl => pl.copy(status = LinkStatus.UnChanged))
      ProjectDAO.updateProjectLinksToDB(updatedProjectLinks, "U")
      val validationErrors = ProjectValidator.validateProject(project, ProjectDAO.getProjectLinks(project.id))

      validationErrors.size shouldNot be(0)
      validationErrors.count(_.validationError.value == ErrorInValidationOfUnchangedLinks.value) should be(1)
    }
  }

  test("validator should return errors if discontinuity is 3 and next road part ely is equal") {
    runWithRollback {
      testDataForElyTest01()
      val testRoad = {(16320L, 1L, "name")}
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.UnChanged, Seq(0L, 10L, 20L, 30L, 40L), changeTrack = false, Seq(testRoad), Discontinuity.ChangingELYCode)

      val validationErrors = ProjectValidator.checkProjectElyCodes(project, projectLinks)
      validationErrors.size should be(1)
      validationErrors.head.validationError.value should be(RoadNotEndingInElyBorder.value)
    }
  }

  test("validator should return errors if discontinuity is anything BUT 3 and next road part ely is different") {
    runWithRollback {
      testDataForElyTest02()
      val testRoad = {(27L, 19L, "name")}
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.UnChanged, Seq(0L, 10L, 20L, 30L, 40L), changeTrack = false, Seq(testRoad), Discontinuity.Continuous, 12L)

      val validationErrors = ProjectValidator.checkProjectElyCodes(project, projectLinks)
      validationErrors.size should be(1)
      validationErrors.head.validationError.value should be(RoadContinuesInAnotherEly.value)
    }
  }

  test("project track codes should be consistent") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L), changeTrack = true)
      val validationErrors = ProjectValidator.checkTrackCode(project, projectLinks)
      validationErrors.size should be(0)
    }
  }

  test("project track codes inconsistent in midle of track") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L), changeTrack = true)
      val inconsistentLinks = projectLinks.map { l =>
        if (l.startAddrMValue == 20 && l.track == Track.RightSide)
          l.copy(track = Track.LeftSide)
        else l
      }
      val validationErrors = ProjectValidator.checkTrackCode(project, inconsistentLinks)
      validationErrors.size should be(1)
    }
  }

  test("project track codes inconsistent in extermities") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L), changeTrack = true)
      val inconsistentLinks = projectLinks.map { l =>
        if (l.startAddrMValue == 0 && l.track == Track.RightSide)
          l.copy(startAddrMValue = 5)
        else l
      }
      val validationErrors = ProjectValidator.checkTrackCode(project, inconsistentLinks)
      validationErrors.size should be(1)
    }
  }

  test("project track codes should be consistent when adding one simple link with track Combined") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L))
      val validationErrors = ProjectValidator.checkTrackCode(project, projectLinks)
      validationErrors.size should be(0)
    }
  }


  test("project track codes should be consistent when adding one simple link with track Combined") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L))
      val validationErrors = ProjectValidator.checkTrackCode(project, projectLinks)
      validationErrors.size should be(0)
    }
  }

  test("Minor discontinuous end ramp road between parts (of any kind) should not give error") {
    runWithRollback {
      val project = util.setUpProjectWithRampLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L))
      val projectLinks = ProjectDAO.getProjectLinks(project.id)
      val errors = ProjectValidator.checkRampContinuityCodes(project, projectLinks)
      errors should have size 0
      val (starting, last) = projectLinks.splitAt(3)
      val ra = Seq(
        RoadAddress(NewRoadAddress, 39998L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous,
          0L, 10L, Some(DateTime.now()), None, None, 0L, 39398L, 0.0, 10.0, AgainstDigitizing, 0L,
          (Some(CalibrationPoint(39398L, 0.0, 0L)), Some(CalibrationPoint(39398L, 10.0, 10L))),
          floating = false, Seq(Point(2.0, 30.0), Point(0.0, 40.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0),
        RoadAddress(NewRoadAddress, 39998L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.Continuous,
          10L, 20L, Some(DateTime.now()), None, None, 0L, 39398L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39398L, 0.0, 0L)), Some(CalibrationPoint(39398L, 10.0, 10L))),
          floating = false, Seq(Point(2.0, 30.0), Point(7.0, 35.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0),
        RoadAddress(NewRoadAddress, 39998L, 1L, RoadType.PublicRoad, Track.Combined, Discontinuity.EndOfRoad,
          20L, 30L, Some(DateTime.now()), None, None, 0L, 39399L, 0.0, 10.0, TowardsDigitizing, 0L,
          (Some(CalibrationPoint(39399L, 0.0, 0L)), Some(CalibrationPoint(39399L, 10.0, 10L))),
          floating = false, Seq(Point(7.0, 35.0), Point(0.0, 40.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0))
      RoadAddressDAO.create(ra)

      ProjectDAO.reserveRoadPart(project.id, 39999L, 20L, "u")
      ProjectDAO.create((starting ++ last.map(_.copy(discontinuity = Discontinuity.EndOfRoad)))
        .map(_.copy(id = NewRoadAddress, roadPartNumber = 20L, createdBy = Some("I"))))
      val updProject = ProjectDAO.getRoadAddressProjectById(project.id).get
      ProjectValidator.checkRampContinuityCodes(updProject,
        starting ++ last.map(_.copy(discontinuity = Discontinuity.MinorDiscontinuity))) should have size 0
    }
  }

  test("Project Links could be both Minor discontinuity or Discontinuous if next part exists in road address / project link table and is not connected") {
    runWithRollback {
      val (project, projectLinks) = util.setUpProjectWithLinks(LinkStatus.New, Seq(0L, 10L, 20L, 30L, 40L))
      val raId = RoadAddressDAO.create(Seq(RoadAddress(NewRoadAddress, 19999L, 2L, RoadType.PublicRoad, Track.Combined, Discontinuity.EndOfRoad,
        0L, 10L, Some(DateTime.now()), None, None, 0L, 39399L, 0.0, 10.0, TowardsDigitizing, 0L, (Some(CalibrationPoint(39399L, 0.0, 0L)), Some(CalibrationPoint(39399L, 10.0, 10L))),
        floating = false, Seq(Point(10.0, 40.0), Point(10.0, 50.0)), LinkGeomSource.ComplimentaryLinkInterface, 8L, NoTermination, 0))).head
      val errors = ProjectValidator.checkOrdinaryRoadContinuityCodes(project, projectLinks)
      errors should have size 1
      errors.head.validationError should be(MajorDiscontinuityFound)

      val (starting, last) = projectLinks.splitAt(3)
      val errorsUpd = ProjectValidator.checkOrdinaryRoadContinuityCodes(project,
        starting ++ last.map(_.copy(discontinuity = Discontinuity.Discontinuous)))
      errorsUpd should have size 0

      val errorsUpd2 = ProjectValidator.checkOrdinaryRoadContinuityCodes(project,
        starting ++ last.map(_.copy(discontinuity = Discontinuity.MinorDiscontinuity)))
      errorsUpd2 should have size 0
    }
  }

}
