package fi.liikennevirasto.viite.model

import fi.liikennevirasto.digiroad2.GeometryUtils
import fi.liikennevirasto.digiroad2.asset.LinkGeomSource
import fi.liikennevirasto.digiroad2.asset.SideCode.{AgainstDigitizing, TowardsDigitizing}
import fi.liikennevirasto.digiroad2.linearasset.GraphPartitioner
import fi.liikennevirasto.viite.dao.{LinkStatus, ProjectLink}
import fi.liikennevirasto.viite.process.InvalidGeometryException

import scala.Predef.augmentString

object ProjectLinkPartitioner extends GraphPartitioner {

  def partition[T <: ProjectAddressLinkLike](projectLinks: Seq[T]): Seq[Seq[T]] = {
    val (splitLinks, links) = projectLinks.partition(_.isSplit)
    // Group by suravage link id
    val splitGroups = splitLinks.groupBy(sl =>
      if (sl.roadLinkSource == LinkGeomSource.NormalLinkInterface)
        sl.linkId else sl.connectedLinkId.get)
    val (outside, inProject) = links.partition(_.status == LinkStatus.Unknown)
    val inProjectGroups = inProject.groupBy(l => (l.status, l.roadNumber, l.roadPartNumber, l.trackCode, l.roadType))
    val (outsideWithRoadName, outsideWithoutRoadName) = outside.partition(link => link.VVHRoadName.get != "none")
    val groupedUnnamedRoads = groupRoadsWithoutName(Seq(), Seq(), outsideWithoutRoadName)
    val outsideGroup = outsideWithRoadName.groupBy(link => (link.roadLinkSource, link.partitioningName))
    val clusters = for (linkGroup <- inProjectGroups.values.toSeq ++ outsideGroup.values.toSeq;
                        cluster <- clusterLinks(linkGroup, fi.liikennevirasto.viite.MaxDistanceForConnectedLinks)) yield cluster
    clusters.map(linksFromCluster) ++ splitGroups.values.toSeq ++ groupedUnnamedRoads
  }

  def groupRoadsWithoutName[T <: ProjectAddressLinkLike](ready: Seq[Seq[T]], prepared: Seq[T],  unprocessed: Seq[T]): Seq[Seq[T]] = {
    if(unprocessed.isEmpty){
      ready ++ Seq(prepared)
    }

    else if(prepared.isEmpty){
      val initialLink = findNotConnectedLink(unprocessed).getOrElse(unprocessed.head)
      groupRoadsWithoutName(ready, Seq(initialLink), unprocessed.filterNot(_.linkId == initialLink.linkId))
    }
    else{
      val linksConnectedToPrepared = unprocessed.filter(link => GeometryUtils.areAdjacent(link.geometry, prepared.last.geometry))
      if(linksConnectedToPrepared.lengthCompare(1) == 0){
        groupRoadsWithoutName(ready, prepared ++ Seq(linksConnectedToPrepared.head), unprocessed.filterNot(_.linkId == linksConnectedToPrepared.head.linkId))
      }
      else {
        groupRoadsWithoutName(ready ++ Seq(prepared), Seq(), unprocessed)
      }
    }
  }

  def findNotConnectedLink[T <: ProjectAddressLinkLike](unprocessed: Seq[T]): Option[T] = {
    unprocessed.find(link =>{
      !unprocessed.filterNot(_.linkId == link.linkId).flatMap(_.geometry).contains(link.geometry.head) || !unprocessed.filterNot(_.linkId == link.linkId).flatMap(_.geometry).contains(link.geometry.last)
    })
  }
}
