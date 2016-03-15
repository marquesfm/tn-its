package fi.liikennevirasto.digiroad2.tnits.examples

import java.awt.geom.Rectangle2D
import java.util
import java.util.Base64

import openlr.binary.ByteArray
import openlr.encoder.{OpenLREncoder, OpenLREncoderParameter}
import openlr.location.LocationFactory
import openlr.map.{Line, MapDatabase, Node}
import openlr.map.loader.{MapLoadParameter, OpenLRMapLoader}
import openlr.map.sqlite.loader.{DBFileNameParameter, SQLiteMapLoader}

class DigiroadMapDatabase extends MapDatabase {
  override def hasTurnRestrictions: Boolean = ???

  override def getAllNodes: util.Iterator[Node] = ???

  override def getLine(id: Long): Line = ???

  override def getAllLines: util.Iterator[Line] = ???

  override def hasTurnRestrictionOnPath(path: util.List[_ <: Line]): Boolean = ???

  override def findNodesCloseByCoordinate(longitude: Double, latitude: Double, distance: Int): util.Iterator[Node] = ???

  override def findLinesCloseByCoordinate(longitude: Double, latitude: Double, distance: Int): util.Iterator[Line] = ???

  override def getMapBoundingBox: Rectangle2D.Double = ???

  override def getNumberOfLines: Int = ???

  override def getNumberOfNodes: Int = ???

  override def getNode(id: Long): Node = ???
}

class DigiroadMapLoader extends OpenLRMapLoader {
  override def getParameter: util.Collection[MapLoadParameter] = ???

  override def getName: String = ???

  override def getDescription: String = ???

  override def getNumberOfParams: Int = ???

  override def load(params: util.Collection[MapLoadParameter]): MapDatabase = {
    new DigiroadMapDatabase
  }

  override def getMapDescriptor: String = ???
}

object OpenLRTest {

  def main(args: Array[String]) = {
    import collection.JavaConverters._

    val mapLoader = new SQLiteMapLoader
    val dbFileName = new DBFileNameParameter()
    dbFileName.setValue("tomtom-openlr-testdata-utrecht/tomtom_utrecht_2008_04_copy.db3")
    val mapDatabase = mapLoader.load(Seq[MapLoadParameter](dbFileName).asJava)
    val encoder = new OpenLREncoder
    val param = new OpenLREncoderParameter.Builder()
      .`with`(mapDatabase)
      .buildParameter()
    val lines = Seq(mapDatabase.getLine(15280002805007L)).asJava
    val lineLocation = LocationFactory.createLineLocation("loc", lines)
    val encoded = encoder.encodeLocation(param, lineLocation)
    val reference = encoded.getLocationReference("binary")
    val data = reference.getLocationReferenceData.asInstanceOf[ByteArray]
    println(new String(Base64.getEncoder.encode(data.getData), "ASCII"))
  }

}