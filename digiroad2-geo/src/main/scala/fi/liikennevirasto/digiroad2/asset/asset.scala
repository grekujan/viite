package fi.liikennevirasto.digiroad2.asset

import org.joda.time.LocalDate

case class AssetType(id: Long, assetTypeName: String, geometryType: String)
case class Asset(id: Long, assetTypeId: Long, lon: Double, lat: Double, roadLinkId: Long,
                 imageIds: Seq[String] = List(), bearing: Option[Int] = None, validityDirection: Option[Int] = None,
                 status: Option[String] = None, readOnly: Boolean = true,
                 municipalityNumber: Option[Long] = None, validityPeriod: Option[String] = None)

case class AssetWithProperties(id: Long, assetTypeId: Long, lon: Double, lat: Double, roadLinkId: Long,
                 imageIds: Seq[String] = List(), bearing: Option[Int] = None, validityDirection: Option[Int] = None,
                 status: Option[String] = None, readOnly: Boolean = true,
                 municipalityNumber: Option[Long] = None,
                 propertyData: Seq[Property] = List(), validityPeriod: Option[String] = None)

case class Property(propertyId: String, propertyName: String, propertyType: String, required: Boolean = false, values: Seq[PropertyValue])
case class PropertyValue(propertyValue: Long, propertyDisplayValue: String, imageId: String = null)
case class EnumeratedPropertyValue(propertyId: String, propertyName: String, propertyType: String, required: Boolean = false, values: Seq[PropertyValue])
case class RoadLink(id: Long, lonLat: Seq[(Double, Double)], endDate: Option[LocalDate] = None, municipalityNumber: Long)

object PropertyTypes {
  val SingleChoice = "single_choice"
  val MultipleChoice = "multiple_choice"
  val Text = "text"
  val ReadOnlyText = "read_only_text"
  val Date = "date"
}

object AssetStatus {
  val Floating = "floating"
}

object ValidityPeriod {
  val Past = "past"
  val Current = "current"
  val Future = "future"
}
