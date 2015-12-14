package fi.liikennevirasto.digiroad2

import fi.liikennevirasto.digiroad2.pointasset.oracle._
import org.joda.time.DateTime

case class NewRailwayCrossing(lon: Double, lat: Double, mmlId: Long, railwayCrossingType: Int, name: String) extends IncomingPointAsset

case class RailwayCrossing(id: Long, mmlId: Long,
                    lon: Double, lat: Double,
                    mValue: Double, floating: Boolean,
                    municipalityCode: Int,
                    railwayCrossingType: Int,
                    name: String,
                    createdBy: Option[String] = None,
                    createdAt: Option[DateTime] = None,
                    modifiedBy: Option[String] = None,
                    modifiedAt: Option[DateTime] = None) extends PointAsset

class RailwayCrossingService(val vvhClient: VVHClient) extends PointAssetOperations {
  type IncomingAsset = NewRailwayCrossing
  type Asset = RailwayCrossing
  type PersistedAsset = PersistedRailwayCrossing

  override def typeId: Int = 230

  override def fetchPointAssets(queryFilter: String => String): Seq[PersistedRailwayCrossing] = OracleRailwayCrossingDao.fetchByFilter(queryFilter)

  override def persistedAssetToAsset(persistedAsset: PersistedRailwayCrossing, floating: Boolean) = {
    RailwayCrossing(
      id = persistedAsset.id,
      mmlId = persistedAsset.mmlId,
      municipalityCode = persistedAsset.municipalityCode,
      lon = persistedAsset.lon,
      lat = persistedAsset.lat,
      mValue = persistedAsset.mValue,
      floating = floating,
      railwayCrossingType = persistedAsset.railwayCrossingType,
      name = persistedAsset.name,
      createdBy = persistedAsset.createdBy,
      createdAt = persistedAsset.createdDateTime,
      modifiedBy = persistedAsset.modifiedBy,
      modifiedAt = persistedAsset.modifiedDateTime)
  }

  override def create(asset: NewRailwayCrossing, username: String, geometry: Seq[Point], municipality: Int): Long = {
    val mValue = GeometryUtils.calculateLinearReferenceFromPoint(Point(asset.lon, asset.lat, 0), geometry)
    withDynTransaction {
      OracleRailwayCrossingDao.create(RailwayCrossingToBePersisted(asset.mmlId, asset.lon, asset.lat, mValue, municipality, username, asset.railwayCrossingType, asset.name), username)
    }
  }

  override def update(id:Long, updatedAsset: NewRailwayCrossing, geometry: Seq[Point], municipality: Int, username: String): Long = {
    val mValue = GeometryUtils.calculateLinearReferenceFromPoint(Point(updatedAsset.lon, updatedAsset.lat, 0), geometry)
    withDynTransaction {
      OracleRailwayCrossingDao.update(id, RailwayCrossingToBePersisted(updatedAsset.mmlId, updatedAsset.lon, updatedAsset.lat, mValue, municipality, username, updatedAsset.railwayCrossingType, updatedAsset.name))
    }
    id
  }
}


