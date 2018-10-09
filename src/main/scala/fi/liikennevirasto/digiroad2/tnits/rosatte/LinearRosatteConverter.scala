package fi.liikennevirasto.digiroad2.tnits.rosatte

import java.io.{BufferedOutputStream, OutputStream, OutputStreamWriter}
import java.time.Instant
import java.util.UUID

import fi.liikennevirasto.digiroad2.tnits.geojson.{Feature, FeatureLinear}
import fi.liikennevirasto.digiroad2.tnits.geometry.{CoordinateTransform, Point}
import fi.liikennevirasto.digiroad2.tnits.openlr.OpenLREncoder
import fi.liikennevirasto.digiroad2.tnits.rosatte.features.{NumericAssetProperties, ProhibitionTypesOperations, RoadLink, ValidityPeriodOperations}
import fi.liikennevirasto.digiroad2.tnits.runners.AssetType

import scala.util.Try
import scala.xml.{Elem, NodeBuffer, NodeSeq}

/** Generates a dataset. */


object LinearRosatteConverter extends AssetRosatteConverter {
  override type FeatureType = FeatureLinear[AssetProperties]
//  type AssetPropertiesType = LinearAssetProperties

  override def applicableDirection(sideCode: Int) : String = {
    sideCode match  {
      case 2 => "inDirection"
      case 3 => "inOppositeDirection"
      case _ => ""
    }
  }

  override def locationReference(feature: FeatureLinear[AssetProperties], reference: String) : NodeBuffer   = {
    val properties = feature.properties
//    writer.write(
    <rst:locationReference>
    <rst:INSPIRELinearLocation gml:id={ UUID.randomUUID().toString }>
        <net:SimpleLinearReference>
          <net:element xlink:href= { DefaultLinkReference + properties.link.id } />
          <net:applicableDirection> { applicableDirection(properties.sideCode) }</net:applicableDirection>
          <net:fromPosition uom="meter"> { properties.startMeasure }</net:fromPosition>
          <net:toPosition uom="meter"> { properties.endMeasure }</net:toPosition>
        </net:SimpleLinearReference>
      </rst:INSPIRELinearLocation>
    </rst:locationReference>
      <rst:locationReference>
        <rst:OpenLRLocationString gml:id= {UUID.randomUUID().toString }>
          <rst:base64String> {reference } </rst:base64String>
          <rst:OpenLRBinaryVersion>1.4</rst:OpenLRBinaryVersion>
        </rst:OpenLRLocationString>
      </rst:locationReference>
  }

  override def properties(assetType: AssetType, feature: FeatureLinear[AssetProperties]) : NodeSeq = {
    assetType.apiEndPoint match {

      case "vehicle_prohibitions" =>
        feature.properties.value.asInstanceOf[Seq[ProhibitionValue]].map { prohibitionValue =>
          <rst:condition>
            <rst:ConditionSet>
              <rst:conditions>
                <rst:VehicleCondition>
                  <rst:negate>false</rst:negate>
                  <rst:vehicleType>
                    {ProhibitionTypesOperations(prohibitionValue.typeId, prohibitionValue.exceptions).vehicleConditionType()}
                  </rst:vehicleType>
                </rst:VehicleCondition>
                <rst:VehicleCondition>
                  <rst:negate>true</rst:negate>{ProhibitionTypesOperations(prohibitionValue.typeId, prohibitionValue.exceptions).vehicleConditionExceptions().map { exception =>
                  <rst:vehicleType>
                  {exception}
                  </rst:vehicleType>
                }}
                </rst:VehicleCondition>
              </rst:conditions>
              <rst:operator>AND</rst:operator>
            </rst:ConditionSet>
            <rst:ConditionSet>
              <rst:conditions>
                <rst:TimeCondition>
                  {prohibitionValue.validityPeriod.map { validityPeriod =>
                  <rst:validityPeriod>
                    <rst:ValidityPeriod>
                      <rst:time>
                        <rst:weekday>
                          <rst:IntegerInterval>
                            <rst:start>
                              {ValidityPeriodOperations(validityPeriod.startHour, validityPeriod.endHour, validityPeriod.days, validityPeriod.startMinute, validityPeriod.endMinute).fromTimeDomainValue()._1}
                            </rst:start>
                            <rst:length>
                              {ValidityPeriodOperations(validityPeriod.startHour, validityPeriod.endHour, validityPeriod.days, validityPeriod.startMinute, validityPeriod.endMinute).fromTimeDomainValue()._2}
                            </rst:length>
                          </rst:IntegerInterval>
                        </rst:weekday>
                        <rst:begin>
                          {s"${validityPeriod.startHour}:${validityPeriod.startMinute}:00"}
                        </rst:begin>
                        <rst:lengthSeconds>
                          {ValidityPeriodOperations(validityPeriod.startHour, validityPeriod.endHour, validityPeriod.days, validityPeriod.startMinute, validityPeriod.endMinute).duration()}
                        </rst:lengthSeconds>
                      </rst:time>
                    </rst:ValidityPeriod>
                  </rst:validityPeriod>
                }}
                </rst:TimeCondition>
              </rst:conditions>
              <rst:operator>OR</rst:operator>
            </rst:ConditionSet>
          </rst:condition>
        }
      case _ =>
        <rst:properties>
          <rst:SafetyFeaturePropertyValue>
            <rst:type>
              {assetType.valueType}
            </rst:type>
            <rst:propertyValue>
              <gml:measure uom={assetType.unit}>
                {feature.properties.value}
              </gml:measure>
            </rst:propertyValue>
          </rst:SafetyFeaturePropertyValue>
        </rst:properties>
    }
  }

  override def geometry(feature: FeatureLinear[AssetProperties] ) : String  = {
    val coordinates = feature.geometry.coordinates.flatMap(_.take(2))
    val transformedCoordinates = CoordinateTransform.convertToWgs84(coordinates)
    transformedCoordinates.mkString(" ")
  }

  override def encodeOpenLRLocationString(feature: FeatureLinear[AssetProperties]): Try[String] = {
    val properties = feature.properties
    val link = properties.link

    val coordinates = link.geometry.coordinates.flatten
    val points = coordinates
        .grouped(3)
        .map { case Seq(x, y, z) => Point(x, y, z) }
        .toSeq

    val linkLength = link.properties.length
    val functionalClass = link.properties.functionalClass
    val linkType = link.properties.`type`
    val (linkGeometry, startM, endM) =
      if (properties.sideCode == 3) //inOppositeDirection
        (points.reverse, linkLength - properties.endMeasure, linkLength - properties.startMeasure)
      else
        (points, properties.startMeasure, properties.endMeasure)

    OpenLREncoder.encodeAssetOnLink(startM, endM, linkGeometry, linkLength, functionalClass, linkType,  DefaultLinkReference + link.id)
  }

//  override def toFeature(feature: Feature[AssetProperties], assetType: AssetType): Any = {
//    toFeatureMember(feature.asInstanceOf[LinearRosatteConverter.FeatureType], assetType)
//  }

//  override def splitFeaturesApplicableToBothDirections(assets: Seq[FeatureLinear[AssetProperties]]): Seq[FeatureLinear[AssetProperties]] = {
//    assets.flatMap { feature =>
//      feature.properties.sideCode match {
//        case 1 =>
//          Seq(feature.copy(properties = feature.properties.setSideCode(sideCode = 2)),
//            feature.copy(properties = feature.properties.setSideCode(sideCode = 3)))
//        case _ =>
//          Seq(feature)
//      }
//    }
//  }

  override def duplicateFeature(feature: FeatureLinear[AssetProperties]) : Seq[FeatureLinear[AssetProperties]] = {
    Seq(feature.copy(properties = feature.properties.setSideCode(sideCode = 2)),
                feature.copy(properties = feature.properties.setSideCode(sideCode = 3)))
  }

}
