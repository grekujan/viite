package fi.liikennevirasto.digiroad2.linearasset.oracle

import fi.liikennevirasto.digiroad2.FeatureClass.AllOthers
import fi.liikennevirasto.digiroad2._
import fi.liikennevirasto.digiroad2.asset.{BoundingRectangle, MultipleCarriageway, Municipality, UnknownDirection}
import fi.liikennevirasto.digiroad2.oracle.OracleDatabase._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{FunSuite, Matchers, Tag}
import org.scalatest.mock.MockitoSugar

import scala.slick.driver.JdbcDriver.backend.Database
import Database.dynamicSession
import scala.slick.jdbc.StaticQuery.interpolation

class OracleLinearAssetDaoSpec extends FunSuite with Matchers {
  val roadLink = VVHRoadlink(388562360, 0, List(Point(0.0, 0.0), Point(0.0, 200.0)), Municipality, UnknownDirection, AllOthers)

  private def daoWithRoadLinks(roadLinks: Seq[VVHRoadlink]): OracleLinearAssetDao = {
    val mockedRoadLinkService = MockitoSugar.mock[RoadLinkService]

    when(mockedRoadLinkService.fetchVVHRoadlinks(roadLinks.map(_.mmlId).toSet))
      .thenReturn(roadLinks)

    roadLinks.foreach { roadLink =>
      when(mockedRoadLinkService.fetchVVHRoadlink(roadLink.mmlId)).thenReturn(Some(roadLink))
    }

    new OracleLinearAssetDao {
      override val roadLinkService: RoadLinkService = mockedRoadLinkService
    }
  }

  private def truncateLinkGeometry(mmlId: Long, startMeasure: Double, endMeasure: Double, roadLinkService: RoadLinkService): Seq[Point] = {
    val geometry = roadLinkService.fetchVVHRoadlink(mmlId).get.geometry
    GeometryUtils.truncateGeometry(geometry, startMeasure, endMeasure)
  }

  def assertSpeedLimitEndPointsOnLink(speedLimitId: Long, mmlId: Long, startMeasure: Double, endMeasure: Double, dao: OracleLinearAssetDao) = {
    val expectedEndPoints = GeometryUtils.geometryEndpoints(truncateLinkGeometry(mmlId, startMeasure, endMeasure, dao.roadLinkService).toList)
    val limitEndPoints = GeometryUtils.geometryEndpoints(dao.getLinksWithLengthFromVVH(20, speedLimitId).find { link => link._1 == mmlId }.get._3)
    expectedEndPoints._1.distanceTo(limitEndPoints._1) should be(0.0 +- 0.01)
    expectedEndPoints._2.distanceTo(limitEndPoints._2) should be(0.0 +- 0.01)
  }

  def passingMunicipalityValidation(code: Int): Unit = {}
  
  def failingMunicipalityValidation(code: Int): Unit = { throw new IllegalArgumentException }

  test("Split should fail when user is not authorized for municipality") {
    Database.forDataSource(ds).withDynTransaction {
      val dao = daoWithRoadLinks(List(roadLink))
      intercept[IllegalArgumentException] {
        dao.splitSpeedLimit(200097, 388562360, 100, 120, "test", failingMunicipalityValidation)
      }
      dynamicSession.rollback()
    }
  }

  test("splitting one link speed limit " +
    "where split measure is after link middle point " +
    "modifies end measure of existing speed limit " +
    "and creates new speed limit for second split", Tag("db")) {
    Database.forDataSource(ds).withDynTransaction {
      val dao = daoWithRoadLinks(List(roadLink))
      val createdId = dao.splitSpeedLimit(200097, 388562360, 100, 120, "test", passingMunicipalityValidation)
      val (existingModifiedBy, _, _, _, _) = dao.getSpeedLimitDetails(200097)
      val (_, _, newCreatedBy, _, _) = dao.getSpeedLimitDetails(createdId)

      assertSpeedLimitEndPointsOnLink(200097, 388562360, 0, 100, dao)
      assertSpeedLimitEndPointsOnLink(createdId, 388562360, 100, 136.788, dao)

      existingModifiedBy shouldBe Some("test")
      newCreatedBy shouldBe Some("test")
      dynamicSession.rollback()
    }
  }

  test("splitting one link speed limit " +
    "where split measure is before link middle point " +
    "modifies start measure of existing speed limit " +
    "and creates new speed limit for first split", Tag("db")) {
    Database.forDataSource(ds).withDynTransaction {
      val dao = daoWithRoadLinks(List(roadLink))
      val createdId = dao.splitSpeedLimit(200097, 388562360, 50, 120, "test", passingMunicipalityValidation)
      val (modifiedBy, _, _, _, _) = dao.getSpeedLimitDetails(200097)
      val (_, _, newCreatedBy, _, _) = dao.getSpeedLimitDetails(createdId)

      assertSpeedLimitEndPointsOnLink(200097, 388562360, 50, 136.788, dao)
      assertSpeedLimitEndPointsOnLink(createdId, 388562360, 0, 50, dao)

      modifiedBy shouldBe Some("test")
      newCreatedBy shouldBe Some("test")
      dynamicSession.rollback()
    }
  }

  test("can update speedlimit value") {
    Database.forDataSource(ds).withDynTransaction {
      val dao = daoWithRoadLinks(List(roadLink))
      dao.updateSpeedLimitValue(200097, 60, "test", _ => ())
      dao.getSpeedLimitDetails(200097)._5 should equal(Some(60))
      dao.updateSpeedLimitValue(200097, 100, "test", _ => ())
      dao.getSpeedLimitDetails(200097)._5 should equal(Some(100))
      dynamicSession.rollback()
    }
  }

  test("filter out floating speed limits") {
    Database.forDataSource(ds).withDynTransaction {
      sqlu"""update asset set floating = 1 where id=200097""".execute()
      val roadLink = VVHRoadLinkWithProperties(388562360, List(Point(0.0, 0.0), Point(0.0, 200.0)), 200.0, Municipality, 0, UnknownDirection, MultipleCarriageway, None, None)
      val mockedRoadLinkService = MockitoSugar.mock[RoadLinkService]
      when(mockedRoadLinkService.getRoadLinksFromVVH(any[BoundingRectangle], any[Set[Int]])).thenReturn(Seq(roadLink))
      when(mockedRoadLinkService.getRoadLinksFromVVH(Set.empty[Long])).thenReturn(Seq.empty[VVHRoadLinkWithProperties])
      val dao = new OracleLinearAssetDao {
        override val roadLinkService: RoadLinkService = mockedRoadLinkService
      }
      val speedLimits = dao.getSpeedLimitLinksByBoundingBox(BoundingRectangle(Point(0.0, 0.0), Point(1.0, 1.0)), Set.empty)
      speedLimits._1 should be(empty)
      dynamicSession.rollback()
    }
  }

  test("speed limit creation fails if speed limit is already defined on link segment") {
    Database.forDataSource(ds).withDynTransaction {
      val dao = daoWithRoadLinks(Nil)
      val id = dao.createSpeedLimit("test", 123, (0.0, 100.0), 1, 40, _ => ())
      id shouldBe defined
      val id2 = dao.createSpeedLimit("test", 123, (0.0, 100.0), 1, 40, _ => ())
      id2 shouldBe None
      dynamicSession.rollback()
    }
  }
}
