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

  private def bookCover(imageSetNode: NodeSeq, name: String): BookCover = {
    val imageName = name + "Image"
    val xml = imageSetNode \ imageName
    xml.size match {
      case 0 => UnknownBookCover()
      case _ => {
        val node = (imageSetNode \ imageName).head
        KnownBookCover(
          name,
          node \ "URL" text,
          node \ "Height" text,
          node \ "Width" text
        )
      }
    }
  }

  def fromXml(xml: Elem): Map[String, BookCover] = {
    val imageSetNode = xml \ "Items" \ "Item" \ "ImageSets" \ "ImageSet"
    Map(
      "Swatch" -> bookCover(imageSetNode, "Swatch"),
      "Small" -> bookCover(imageSetNode, "Small"),
      "Thumbnail" -> bookCover(imageSetNode, "Thumbnail"),
      "Tiny" -> bookCover(imageSetNode, "Tiny"),
      "Medium" -> bookCover(imageSetNode, "Medium"),
      "Large" -> bookCover(imageSetNode, "Large")
    )
  }
}
