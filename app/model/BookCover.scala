package model

import xml.{NodeSeq, Elem}
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

case class BookCover(url: String = BookCover.unknownBookCoverUrl) {

  override def toString: String = {
    url
  }

  def size(size: Int): BookCover = {
    val unknownCover = BookCover.unknownBookCoverUrl
    url match {
      case `unknownCover` => this
      case _ => BookCover(url.replaceAll(".jpg", "._SL" + size + "_.jpg"))
    }
  }

}

object BookCover {

  val unknownBookCoverUrl = "/assets/images/no-image.jpg"

  implicit object BookCoverFormat extends Format[BookCover] {

    def reads(json: JsValue): JsResult[BookCover] = {
      val url = (json \ "url").as[String]
      url match {
        case "Unknown" => JsSuccess(BookCover())
        case _ => JsSuccess(BookCover(url))
      }
    }

    def writes(bookCover: BookCover): JsValue = JsObject(List(
      "url" -> JsString(bookCover.url)))

  }

  private def bookCovers(largeImageNode: NodeSeq): BookCover = {
    largeImageNode.size match {
      case 0 => BookCover()
      case _ => BookCover(largeImageNode \ "URL" text)
    }
  }

  def fromAmazonXml(xml: Elem): BookCover = {
    val largeImageNode = xml \ "Items" \ "Item" \ "LargeImage"
    bookCovers(largeImageNode)
  }

}
