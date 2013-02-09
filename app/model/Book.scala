package model

import xml.{NodeSeq, Elem}
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

sealed trait Book extends Ordered[Book] {

  def isbn: Option[String]

  def ean: Option[String]

  def title: String

  def authors: Option[String]

  def binding: Option[String]

  def edition: Option[String]

  def numberOfPages: Option[String]

  def publicationDate: Option[String]

  def publisher: Option[String]

  def bookCover: BookCover

  def listPrice: Option[Price]

  def offerSummary: Option[OfferSummary]

  def displayableStringOption(option: Option[String]): String

  def displayableListPrice: String

  def displayableLowestNewPrice: String

  def displayableLowestUsedPrice: String

  override def toString: String = {
    "TITLE: " + title + "\n" +
      "AUTHORS: " + authors + "\n" +
      "ISBN: " + isbn + "\n" +
      "EAN: " + ean + "\n" +
      "PUBLISHER: " + publisher + "\n" +
      "PUBLICATION DATE: " + publicationDate + "\n" +
      "BINDING: " + binding + "\n" +
      "EDITION:" + edition + "\n" +
      "PAGES: " + numberOfPages + "\n" +
      "LIST PRICE: " + listPrice + "\n" +
      "OFFER SUMMARY: \n\t" + offerSummary + "\n" +
      "COVER: \n\t" + bookCover + "\n"
  }

  def valid: Boolean

  def bookCover(size: Int): BookCover

  def compare(other: Book) = title compare other.title

}

private case class BookImpl(isbn: Option[String],
                            ean: Option[String],
                            title: String,
                            authors: Option[String],
                            binding: Option[String],
                            edition: Option[String],
                            numberOfPages: Option[String],
                            publicationDate: Option[String],
                            publisher: Option[String],
                            bookCover: BookCover,
                            listPrice: Option[Price],
                            offerSummary: Option[OfferSummary]) extends Book {

  def valid: Boolean = true

  def noData = "- no data -"

  def displayableStringOption(option: Option[String]): String =
    option match {
      case None => noData
      case Some(text) => text
    }

  def bookCover(size: Int): BookCover = {
    bookCover.size(size)
  }

  def displayableListPrice: String =
    listPrice match {
      case None => noData
      case Some(listPriceMatch) => listPriceMatch.formattedPrice
    }

  def displayableLowestNewPrice: String =
    offerSummary match {
      case None => noData
      case Some(offerSummaryMatch) => {
        offerSummaryMatch.lowestNewPrice match {
          case None => noData
          case Some(price) => price.formattedPrice
        }
      }
    }

  def displayableLowestUsedPrice: String =
    offerSummary match {
      case None => noData
      case Some(offerSummaryMatch) => {
        offerSummaryMatch.lowestUsedPrice match {
          case None => noData
          case Some(price) => price.formattedPrice
        }
      }
    }
}

object Book {

  implicit object BookFormat extends Format[Book] {

    def reads(json: JsValue): JsResult[Book] = JsSuccess(BookImpl(
      (json \ "isbn").as[Option[String]],
      (json \ "ean").as[Option[String]],
      (json \ "title").as[String],
      (json \ "authors").as[Option[String]],
      (json \ "binding").as[Option[String]],
      (json \ "edition").as[Option[String]],
      (json \ "numberOfPages").as[Option[String]],
      (json \ "publicationDate").as[Option[String]],
      (json \ "publisher").as[Option[String]],
      BookCover.BookCoverFormat.reads(json \ "bookCover").get,
      Price.PriceFormat.reads(json \ "listPrice").get,
      OfferSummary.OfferSummaryFormat.reads(json \ "offerSummary").get))

    def writes(book: Book): JsValue = JsObject(List(
      "isbn" -> Json.toJson(book.isbn),
      "ean" -> Json.toJson(book.ean),
      "title" -> JsString(book.title),
      "authors" -> Json.toJson(book.authors),
      "binding" -> Json.toJson(book.binding),
      "edition" -> Json.toJson(book.edition),
      "numberOfPages" -> Json.toJson(book.numberOfPages),
      "publicationDate" -> Json.toJson(book.publicationDate),
      "publisher" -> Json.toJson(book.publisher),
      "bookCover" -> BookCover.BookCoverFormat.writes(book.bookCover),
      "listPrice" -> Price.PriceFormat.writes(book.listPrice),
      "offerSummary" -> OfferSummary.OfferSummaryFormat.writes(book.offerSummary)))
  }

  private def getOptionText(node: NodeSeq): Option[String] =
    node.headOption match {
      case None => None
      case Some(aNode) => Some(aNode.text)
    }

  def fromAmazonXml(isbn: String, xml: Elem): Option[Book] = {
    (xml \\ "Error").size match {
      case 0 => {
        val itemAttributesNode = xml \ "Items" \ "Item" \ "ItemAttributes"
        val authorsString = (itemAttributesNode \ "Author" map (f => f.text) mkString (", "))
        val authors = authorsString match {
          case "" => None
          case _ => Some(authorsString)
        }
        Some(new BookImpl(
          getOptionText(itemAttributesNode \ "ISBN"),
          getOptionText(itemAttributesNode \ "EAN"),
          itemAttributesNode \ "Title" text,
          authors,
          getOptionText(itemAttributesNode \ "Binding"),
          getOptionText(itemAttributesNode \ "Edition"),
          getOptionText(itemAttributesNode \ "NumberOfPages"),
          getOptionText(itemAttributesNode \ "PublicationDate"),
          getOptionText(itemAttributesNode \ "Publisher"),
          BookCover.fromAmazonXml(xml),
          Price.fromAmazonXml(itemAttributesNode \ "ListPrice"),
          OfferSummary.fromAmazonXml(xml)
        ))
      }
      case _ => {
        println("Could not find book: " + isbn)
        None
      }
    }
  }

}
