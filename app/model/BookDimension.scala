package model

import xml.{NodeSeq, Elem}
import scala.Float

sealed trait BookDimension

case class KnownBookDimension(
                               height: Float, // hundredths-inches
                               length: Float, // hundredths-inches
                               width: Float, // hundredths-inches
                               weight: Float // hundredths-pounds
                               ) extends BookDimension {

  override def toString: String = {
    "HEIGHT: " + height / 100 + " inches, " +
      "LENGTH: " + length / 100 + " inches, " +
      "WIDTH: " + width / 100 + " inches, " +
      "WEIGHT: " + weight / 100 + " lb"
  }

}

case class UnknownBookDimension() extends BookDimension {

  override def toString: String = {
    "Unknown dimensions"
  }

}

object BookDimension {

  private def getInt(packageDimensionsNode: NodeSeq, dimension: String): Float = {
    (packageDimensionsNode \ dimension).text match {
      case "" => 0
      case _ => (packageDimensionsNode \ dimension text).toFloat
    }
  }

  def fromXml(xml: Elem): BookDimension = {
    val packageDimensionsNode = xml \ "Items" \ "Item" \ "ItemAttributes" \ "ItemDimensions"
    packageDimensionsNode.size match {
      case 0 => UnknownBookDimension()
      case _ => KnownBookDimension(
        getInt(packageDimensionsNode, "Height"),
        getInt(packageDimensionsNode, "Length"),
        getInt(packageDimensionsNode, "Width"),
        getInt(packageDimensionsNode, "Weight")
      )
    }
  }
}
