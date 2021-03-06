package fi.liikennevirasto.digiroad2.tnits.rosatte

import fi.liikennevirasto.digiroad2.tnits.geojson

/** GeoJSON types specialized to OTH assets. */
object features {
  type Asset = geojson.Feature[AssetProperties]
  type RoadLink = geojson.Feature[RoadLinkProperties]

  case class AssetProperties(
    sideCode: Int,
    changeType: String,
    value: Any,
    startMeasure: Double,
    endMeasure: Double,
    link: RoadLink)

  case class RoadLinkProperties(
    functionalClass: Int,
    `type`: Int,
    length: Double)
}
