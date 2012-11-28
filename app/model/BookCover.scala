package model

import xml.Elem

case class BookCover(
                      size: String,
                      url: String,
                      height: Int, // pixels
                      width: Int // pixels
                      ) {

  override def toString: String = {
    "HEIGHT: " + height + ", " +
      "WIDTH: " + width + ", " +
      "URL: " + url
  }

}

object BookCover {

  def fromXml(xml: Elem): Map[String, BookCover] = {
    val imageSetNode = xml \ "Items" \ "Item" \ "ImageSets" \ "ImageSet"
    Map(
      "Swatch" -> new BookCover(
        "Swatch",
        imageSetNode \ "SwatchImage" \ "URL" text,
        (imageSetNode \ "SwatchImage" \ "Height" text) toInt,
        (imageSetNode \ "SwatchImage" \ "Width" text) toInt
      ),
      "Small" -> new BookCover(
        "Small",
        imageSetNode \ "SmallImage" \ "URL" text,
        (imageSetNode \ "SmallImage" \ "Height" text) toInt,
        (imageSetNode \ "SmallImage" \ "Width" text) toInt
      ),
      "Thumbnail" -> new BookCover(
        "Thumbnail",
        imageSetNode \ "ThumbnailImage" \ "URL" text,
        (imageSetNode \ "ThumbnailImage" \ "Height" text) toInt,
        (imageSetNode \ "ThumbnailImage" \ "Width" text) toInt
      ),
      "Tiny" -> new BookCover(
        "Tiny",
        imageSetNode \ "TinyImage" \ "URL" text,
        (imageSetNode \ "TinyImage" \ "Height" text) toInt,
        (imageSetNode \ "TinyImage" \ "Width" text) toInt
      ),
      "Medium" -> new BookCover(
        "Medium",
        imageSetNode \ "MediumImage" \ "URL" text,
        (imageSetNode \ "MediumImage" \ "Height" text) toInt,
        (imageSetNode \ "MediumImage" \ "Width" text) toInt
      ),
      "Large" -> new BookCover(
        "Large",
        imageSetNode \ "LargeImage" \ "URL" text,
        (imageSetNode \ "LargeImage" \ "Height" text) toInt,
        (imageSetNode \ "LargeImage" \ "Width" text) toInt
      )
    )
  }
}
