package model

import xml.{NodeSeq, Elem}
import play.api.libs.json.{JsString, JsObject, JsValue, Format}

sealed trait BookCover {
  def url: String

  def size(size: Int): BookCover
}

private case class KnownBookCover(url: String) extends BookCover {

  override def toString: String = {
    url
  }

  def size(size: Int): BookCover = {
    KnownBookCover(url.replaceAll(".jpg", "._SL" + size + "_.jpg"))
  }

}

private case class UnknownBookCover() extends BookCover {

  def url: String = "Unknown"

  override def toString: String = {
    "/assets/images/no-image.jpg"
  }

  def size(size: Int): BookCover = this

}

object BookCover {

  implicit object BookCoverFormat extends Format[BookCover] {

    def reads(json: JsValue): BookCover = {
      val url = (json \ "url").as[String]
      url match {
        case "Unknown" => UnknownBookCover()
        case _ => KnownBookCover(url)
      }
    }

    def writes(bookCover: BookCover): JsValue = JsObject(List(
      "url" -> JsString(bookCover.url)))

  }

  private def bookCovers(largeImageNode: NodeSeq): BookCover = {
    largeImageNode.size match {
      case 0 => UnknownBookCover()
      case _ => KnownBookCover(largeImageNode \ "URL" text)
    }
  }

  def fromAmazonXml(xml: Elem): BookCover = {
    val largeImageNode = xml \ "Items" \ "Item" \ "LargeImage"
    bookCovers(largeImageNode)
  }

}
