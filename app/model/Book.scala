package model

import xml.Elem
import play.api.libs.json.{JsString, JsObject, JsValue, Format}

sealed trait Book extends Ordered[Book] {

  def isbn: String

  def ean: String

  def authors: String

  def binding: String

  def numberOfPages: String

  def publicationDate: String

  def publisher: String

  def title: String

  def bookCover: BookCover

  def listPrice: Price

  def offerSummary: OfferSummary

  override def toString: String = {
    "TITLE: " + title + "\n" +
      "AUTHORS: " + authors + "\n" +
      "ISBN: " + isbn + "\n" +
      "EAN: " + ean + "\n" +
      "PUBLISHER: " + publisher + "\n" +
      "PUBLICATION DATE: " + publicationDate + "\n" +
      "BINDING: " + binding + "\n" +
      "PAGES: " + numberOfPages + "\n" +
      "LIST PRICE: " + listPrice + "\n" +
      "OFFER SUMMARY: \n\t" + offerSummary + "\n" +
      "COVER: \n\t" + bookCover + "\n"
  }

  def valid: Boolean

  def bookCover(size: Int): BookCover

  def compare(other: Book) = title compare other.title

}

private case class KnownBook(isbn: String,
                             ean: String,
                             authors: String,
                             binding: String,
                             numberOfPages: String,
                             publicationDate: String,
                             publisher: String,
                             title: String,
                             bookCover: BookCover,
                             listPrice: Price,
                             offerSummary: OfferSummary) extends Book {
  def valid: Boolean = true

  def bookCover(size: Int): BookCover = {
    bookCover.size(size)
  }
}

private case class UnknownBook(isbn: String) extends Book {

  def ean: String = "Unknown"

  def authors: String = "Unknown"

  def binding: String = "Unknown"

  def numberOfPages: String = "Unknown"

  def publicationDate: String = "Unknown"

  def publisher: String = "Unknown"

  def title: String = "Unknown"

  def bookCover: BookCover = UnknownBookCover()

  def listPrice: Price = UnknownPrice()

  def offerSummary: OfferSummary = UnknownOfferSummary()

  def valid: Boolean = false

  def bookCover(size: Int): BookCover = UnknownBookCover()

}

object Book {

  implicit object BookFormat extends Format[Book] {

    def reads(json: JsValue): Book = KnownBook(
      (json \ "isbn").as[String],
      (json \ "ean").as[String],
      (json \ "authors").as[String],
      (json \ "binding").as[String],
      (json \ "numberOfPages").as[String],
      (json \ "publicationDate").as[String],
      (json \ "publisher").as[String],
      (json \ "title").as[String],
      BookCover.BookCoverFormat.reads(json \ "bookCover"),
      Price.PriceFormat.reads(json \ "listPrice"),
      OfferSummary.OfferSummaryFormat.reads(json \ "offerSummary"))

    def writes(book: Book): JsValue = JsObject(List(
      "isbn" -> JsString(book.isbn),
      "ean" -> JsString(book.ean),
      "authors" -> JsString(book.authors),
      "binding" -> JsString(book.binding),
      "numberOfPages" -> JsString(book.numberOfPages),
      "publicationDate" -> JsString(book.publicationDate),
      "publisher" -> JsString(book.publisher),
      "title" -> JsString(book.title),
      "bookCover" -> BookCover.BookCoverFormat.writes(book.bookCover),
      "listPrice" -> Price.PriceFormat.writes(book.listPrice),
      "offerSummary" -> OfferSummary.OfferSummaryFormat.writes(book.offerSummary)))

  }

  def fromAmazonXml(isbn: String, xml: Elem): Book = {
    (xml \\ "Error").size match {
      case 0 => {
        val itemAttributesNode = xml \ "Items" \ "Item" \ "ItemAttributes"
        val pages = (itemAttributesNode \ "NumberOfPages").text match {
          case "" => "Unknown"
          case _ => (itemAttributesNode \ "NumberOfPages").text
        }
        new KnownBook(
          itemAttributesNode \ "ISBN" text,
          itemAttributesNode \ "EAN" text,
          itemAttributesNode \ "Author" map (f => f.text) mkString (", "),
          itemAttributesNode \ "Binding" text,
          pages,
          itemAttributesNode \ "PublicationDate" text,
          itemAttributesNode \ "Publisher" text,
          itemAttributesNode \ "Title" text,
          BookCover.fromAmazonXml(xml),
          Price.fromAmazonXml(itemAttributesNode \ "ListPrice"),
          OfferSummary.fromAmazonXml(xml)
        )
      }
      case _ => {
        println("Could not find book: " + isbn)
        UnknownBook(isbn)
      }
    }
  }

}
