package fi.liikennevirasto.digiroad2

import fi.liikennevirasto.digiroad2.linearasset.{RoadLink, RoadLinkLike}
import fi.liikennevirasto.digiroad2.util.MunicipalityCodeImporter
import org.joda.time.DateTime

object PointAssetFiller {
  case class testPersistedPointAsset(id: Long, lon: Double, lat: Double, municipalityCode: Int, linkId: Long, mValue: Double, floating: Boolean, vvhTimeStamp: Long) extends PersistedPointAsset


  case class AssetAdjustment(assetId: Long, lon: Double, lat: Double, linkId: Long, mValue: Double, floating: Boolean)
  private val MaxDistanceDiffAllowed = 3.0

   def correctRoadLinkAndGeometry(asset: PersistedPointAsset , roadLinks: Seq[RoadLink], changeInfo: ChangeInfo, adjustmentOption: Option[AssetAdjustment]): Option[AssetAdjustment]={
      val newRoadLink = roadLinks.filter(_.linkId == changeInfo.newId.getOrElse(0L)).head
      val points = GeometryUtils.geometryEndpoints(newRoadLink.geometry)

       val assetPoint = adjustmentOption match {
         case Some(adjustment) =>
           Point(adjustment.lon, adjustment.lat)
       case _ =>
         Point(asset.lon, asset.lat)
     }


    val pointToIni = Seq(assetPoint, points._1)
    val pointToEnd = Seq(assetPoint, points._2)
    val distBetweenPointEnd = GeometryUtils.geometryLength(pointToEnd)

    val newAssetPointOption = GeometryUtils.geometryLength(pointToIni) match {

      case iniDist if iniDist > MaxDistanceDiffAllowed && distBetweenPointEnd <= MaxDistanceDiffAllowed => Some(points._2)
      case iniDist if iniDist <= MaxDistanceDiffAllowed && iniDist <= distBetweenPointEnd => Some(points._1)
      case iniDist if iniDist <= MaxDistanceDiffAllowed && iniDist > distBetweenPointEnd => Some(points._2)
      case iniDist if iniDist <= MaxDistanceDiffAllowed => Some(points._1)
      case _ =>  None
    }

    newAssetPointOption match {
      case Some(newAssetPoint) =>
        val mValue = GeometryUtils.calculateLinearReferenceFromPoint(newAssetPoint, newRoadLink.geometry)
        correctGeometry(asset.id, newRoadLink, mValue)
      case _ =>
        None
    }

  }

  def correctedPersistedAsset(asset: PersistedPointAsset, roadLinks: Seq[RoadLink], changeInfos: Seq[ChangeInfo]): Option[AssetAdjustment] = {
    val pointAssetLastChanges = changeInfos.
      filterNot(changeInfo => changeInfo.newId.isEmpty || changeInfo.oldId.isEmpty).
      filter(changeInfo => changeInfo.oldId.getOrElse(0L) == asset.linkId && changeInfo.vvhTimeStamp > asset.vvhTimeStamp).
      sortBy(- _.vvhTimeStamp)

    pointAssetLastChanges.foldLeft(None: Option[AssetAdjustment]){
      (adjustment, changeInfo) =>
        ChangeType.apply(changeInfo.changeType) match {
          case ChangeType.CombinedModifiedPart | ChangeType.CombinedRemovedPart if changeInfo.newId != changeInfo.oldId => //Geometry Combined
            correctValuesAndGeometry(asset, roadLinks, changeInfo, adjustment)

          case ChangeType.LenghtenedCommonPart | ChangeType.LengthenedNewPart if asset.mValue < changeInfo.newStartMeasure.getOrElse(0.0) => //Geometry Lengthened
            correctValuesAndGeometry(asset, roadLinks, changeInfo, adjustment)

          case ChangeType.DividedModifiedPart | ChangeType.DividedNewPart if (asset.mValue >= changeInfo.oldStartMeasure.getOrElse(0.0)) && (asset.mValue <= changeInfo.oldEndMeasure.getOrElse(0.0)) => //Geometry Divided
            correctValuesAndGeometry(asset, roadLinks, changeInfo, adjustment)

          case ChangeType.ShortenedCommonPart | ChangeType.ShortenedRemovedPart => //Geometry Shortened
            correctRoadLinkAndGeometry(asset, roadLinks, changeInfo, adjustment)

          case _ => adjustment
        }
    }
  }

  private def correctValuesAndGeometry(asset: PersistedPointAsset, roadLinks: Seq[RoadLink], changeInfo: ChangeInfo, adjustmentOption: Option[AssetAdjustment]) = {
    def calculateMvalue(mValue: Double, changeInfo: ChangeInfo)={
      (mValue - changeInfo.oldStartMeasure.getOrElse(0.0)) + changeInfo.newStartMeasure.getOrElse(0.0)
    }
    val newRoadLink = roadLinks.find(_.linkId == changeInfo.newId.getOrElse(0L)).get

    adjustmentOption match {
      case Some(adjustment) =>
        correctGeometry(asset.id, newRoadLink, calculateMvalue(adjustment.mValue, changeInfo))
      case _ =>
        correctGeometry(asset.id, newRoadLink, calculateMvalue(asset.mValue, changeInfo))

    }
  }

  private def correctGeometry(assetId: Long, roadLink: RoadLink, newMValue: Double): Option[AssetAdjustment] = {
    val newAssetPoint = GeometryUtils.calculatePointFromLinearReference(roadLink.geometry, newMValue)
    newAssetPoint match {
      case Some(point) => Some(AssetAdjustment(assetId, point.x, point.y, roadLink.linkId, newMValue, floating = false))
      case _ => None
    }
  }
}