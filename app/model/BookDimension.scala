package model

import xml.Elem

class BookDimension(
                     height: Int, // hundredths-inches
                     length: Int, // hundredths-inches
                     width: Int, // hundredths-inches
                     weight: Int // hundredths-pounds
                     ) {

  override def toString(): String = {
    "HEIGHT: " + height / 100 + " inches, " +
      "LENGTH: " + length / 100 + " inches, " +
      "WIDTH: " + width / 100 + " inches, " +
      "WEIGHT: " + weight / 100 + " lb"
  }

}

object BookDimension {

  def fromXml(xml: Elem): BookDimension = {
    val packageDimensionsNode = xml \ "Items" \ "Item" \ "ItemAttributes" \ "ItemDimensions"
    new BookDimension(
      (packageDimensionsNode \ "Height" text) toInt,
      (packageDimensionsNode \ "Length" text) toInt,
      (packageDimensionsNode \ "Width" text) toInt,
      (packageDimensionsNode \ "Weight" text) toInt
    )
  }
}
