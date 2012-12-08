package model

import xml.Elem

sealed trait Book extends Ordered[Book] {

  def isbn: String

  def ean: String

  def authors: String

  def binding: String

  def numberOfPages: String

  def publicationDate: String

  def publisher: String

  def title: String

  def bookCovers: Map[String, List[BookCover]]

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
      "COVERS: \n\t" + (bookCovers mkString ("\n\t"))
  }

  def valid: Boolean

  def thumbnailCovers: List[BookCover] = bookCovers("Thumbnail")

  def tinyCovers: List[BookCover] = bookCovers("Tiny")

  def mediumCovers: List[BookCover] = bookCovers("Medium")

  def mediumLargeCovers: List[BookCover] = bookCovers("MediumLarge")

  def largeCovers: List[BookCover] = bookCovers("Large")

  def compare(other: Book) = title compare other.title

}

case class KnownBook(isbn: String,
                     ean: String,
                     authors: String,
                     binding: String,
                     numberOfPages: String,
                     publicationDate: String,
                     publisher: String,
                     title: String,
                     bookCovers: Map[String, List[BookCover]],
                     listPrice: Price,
                     offerSummary: OfferSummary) extends Book {
  def valid: Boolean = true
}

case class UnknownBook(isbn: String) extends Book {

  def ean: String = "Unknown"

  def authors: String = "Unknown"

  def binding: String = "Unknown"

  def numberOfPages: String = "Unknown"

  def publicationDate: String = "Unknown"

  def publisher: String = "Unknown"

  def title: String = "Unknown"

  def bookCovers: Map[String, List[BookCover]] = Map()

  def listPrice: Price = UnknownPrice()

  def offerSummary: OfferSummary = UnknownOfferSummary()

  def valid: Boolean = false

}

object Book {

  def fromXml(isbn: String, xml: Elem): Book = {
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
          BookCover.fromXml(xml),
          Price.fromXml(itemAttributesNode \ "ListPrice"),
          OfferSummary.fromXml(xml)
        )
      }
      case _ => {
        println("Could not find book: " + isbn)
        UnknownBook(isbn)
      }
    }
  }

}
