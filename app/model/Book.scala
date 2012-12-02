package model

import xml.Elem

class Book(
            val isbn: String,
            val ean: String,
            val authors: String,
            val binding: String,
            val numberOfPages: String,
            val publicationDate: String,
            val publisher: String,
            val title: String,
            val dimension: BookDimension,
            val bookCovers: Map[String, BookCover],
            val offerSummary: OfferSummary) {

  override def toString: String = {
    "TITLE: " + title + "\n" +
      "AUTHORS: " + authors + "\n" +
      "ISBN: " + isbn + "\n" +
      "EAN: " + ean + "\n" +
      "PUBLISHER: " + publisher + "\n" +
      "PUBLICATION DATE: " + publicationDate + "\n" +
      "BINDING: " + binding + "\n" +
      "PAGES: " + numberOfPages + "\n" +
      "DIMENSION: \n\t" + dimension + "\n" +
      "OFFER SUMMARY: \n\t" + offerSummary + "\n" +
      "COVERS: \n\t" + (bookCovers mkString ("\n\t"))
  }

  def thumbnailCover(): BookCover = bookCovers("Thumbnail")

  def mediumCover(): BookCover = bookCovers("Medium")

  def largeCover(): BookCover = bookCovers("Large")

}

object Book {

  def fromXml(xml: Elem): Book = {
    val itemAttributesNode = xml \ "Items" \ "Item" \ "ItemAttributes"
    val pages = (itemAttributesNode \ "NumberOfPages").text match {
      case "" => "Unknown"
      case _ => (itemAttributesNode \ "NumberOfPages").text
    }
    new Book(
      itemAttributesNode \ "ISBN" text,
      itemAttributesNode \ "EAN" text,
      itemAttributesNode \ "Author" map (f => f.text) mkString (", "),
      itemAttributesNode \ "Binding" text,
      pages,
      itemAttributesNode \ "PublicationDate" text,
      itemAttributesNode \ "Publisher" text,
      itemAttributesNode \ "Title" text,
      BookDimension.fromXml(xml),
      BookCover.fromXml(xml),
      OfferSummary.fromXml(xml)
    )
  }

}
