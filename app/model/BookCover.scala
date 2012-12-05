package model

import xml.{NodeSeq, Elem}

sealed trait BookCover {
  def size: String

  def url: String

  def height: String

  def width: String
}

case class KnownBookCover(
                           size: String,
                           url: String,
                           height: String, // pixels
                           width: String // pixels
                           ) extends BookCover {

  override def toString: String = {
    "HEIGHT: " + height + ", " +
      "WIDTH: " + width + ", " +
      "URL: " + url
  }

}

case class UnknownBookCover() extends BookCover {

  def size: String = "Unknown"

  def url: String = "Unknown"

  def height: String = "Unknown"

  def width: String = "Unknown"

  override def toString: String = {
    "Unknown"
  }

}

object BookCover {

  private def bookCovers(imageSetNode: NodeSeq, name: String): List[BookCover] = {
    val imageName = name + "Image"
    val xml = imageSetNode \ imageName
    xml.size match {
      case 0 => List(UnknownBookCover())
      case _ => {
        val nodes = (imageSetNode \ imageName)
        (nodes map (node =>
          KnownBookCover(
            name,
            node \ "URL" text,
            node \ "Height" text,
            node \ "Width" text
          )
          )).toList
      }
    }
  }

  def fromXml(xml: Elem): Map[String, List[BookCover]] = {
    val imageSetNode = xml \ "Items" \ "Item" \ "ImageSets" \ "ImageSet"
    Map(
      "Swatch" -> bookCovers(imageSetNode, "Swatch"),
      "Small" -> bookCovers(imageSetNode, "Small"),
      "Thumbnail" -> bookCovers(imageSetNode, "Thumbnail"),
      "Tiny" -> bookCovers(imageSetNode, "Tiny"),
      "Medium" -> bookCovers(imageSetNode, "Medium"),
      "Large" -> bookCovers(imageSetNode, "Large")
    )
  }
}
