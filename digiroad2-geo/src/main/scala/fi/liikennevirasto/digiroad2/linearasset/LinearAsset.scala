package fi.liikennevirasto.digiroad2.linearasset

import fi.liikennevirasto.digiroad2.Point
import fi.liikennevirasto.digiroad2.asset.{TrafficDirection, SideCode}
import org.joda.time.DateTime

trait LinearAsset extends PolyLine {
  val id: Long
  val mmlId: Long
  val sideCode: SideCode
  val value: Option[Int]
}

case class PieceWiseLinearAsset(id: Long, mmlId: Long, sideCode: SideCode, value: Option[Int], geometry: Seq[Point], expired: Boolean,
                                startMeasure: Double, endMeasure: Double,
                                endpoints: Set[Point], modifiedBy: Option[String], modifiedDateTime: Option[DateTime],
                                createdBy: Option[String], createdDateTime: Option[DateTime], typeId: Int, trafficDirection: TrafficDirection) extends LinearAsset

case class PersistedLinearAsset(id: Long, mmlId: Long, sideCode: Int, value: Option[Int],
                         startMeasure: Double, endMeasure: Double, createdBy: Option[String], createdDateTime: Option[DateTime],
                         modifiedBy: Option[String], modifiedDateTime: Option[DateTime], expired: Boolean, typeId: Int)

case class NewLinearAsset(mmlId: Long, startMeasure: Double, endMeasure: Double, value: Option[Int], sideCode: Int)
