package model

import xml.{NodeSeq, Elem}
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.Logger
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.DBObject
import java.text.SimpleDateFormat
import java.util.Date

case class Book(id: Option[String],
                isbn: Option[String],
                ean: Option[String],
                title: String,
                authors: Option[String],
                binding: Option[String],
                edition: Option[String],
                numberOfPages: Option[String],
                publicationDate: Option[String],
                publisher: Option[String],
                smallBookCover: Option[String],
                largeBookCover: Option[String],
                listPrice: Option[Int],
                lowestPrice: Option[Int],
                totalAvailable: Option[Int],
                lastPriceUpdateTimestamp: Option[Long],
                signed: Boolean,
                notes: Option[String]) {

  def noData = "- no data -"

  def noImage = "/assets/images/no-image.jpg"

  def smallBookCoverWithDefault: String =
    if (smallBookCover.isEmpty || smallBookCover.get == "") {
      noImage
    } else {
      smallBookCover.get
    }

  def largeBookCoverWithDefault: String =
    if (largeBookCover.isEmpty || largeBookCover.get == "") {
      noImage
    } else {
      largeBookCover.get
    }

  def displayableStringOption(option: Option[String]): String =
    option match {
      case None => noData
      case Some(text) => text
    }

  def displayableListPrice: String =
    listPrice match {
      case None => noData
      case Some(listPriceMatch) => "£" + (listPriceMatch / 100).toString
    }

  def displayableTotalAvailable: String =
    totalAvailable match {
      case None => "?"
      case Some(available) => available.toString
    }

  def displayableLowestPrice: String =
    lowestPrice match {
      case None => noData
      case Some(price) => "£" + (price / 100).toString
    }

  def displayableLastPriceUpdateTimestamp: String =
    lastPriceUpdateTimestamp match {
      case None => "unknown"
      case Some(timestamp) => Book.dateFormat.format(new Date(timestamp))
    }



  override def toString = Book.BookFormat.writes(this).toString()

}

object Book {

  type Price = Option[Int]
  type TotalAvailable = Option[Int]
  type OfferSummary = (Price, TotalAvailable)

  val dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z")

  implicit def book2DbObject(book: Book): DBObject =
    MongoDBObject(
      "isbn" -> book.isbn,
      "ean" -> book.ean,
      "title" -> book.title,
      "authors" -> book.authors,
      "binding" -> book.binding,
      "edition" -> book.edition,
      "numberOfPages" -> book.numberOfPages,
      "publicationDate" -> book.publicationDate,
      "publisher" -> book.publisher,
      "smallBookCover" -> book.smallBookCover,
      "largeBookCover" -> book.largeBookCover,
      "listPrice" -> book.listPrice,
      "lowestPrice" -> book.lowestPrice,
      "totalAvailable" -> book.totalAvailable,
      "lastPriceUpdateTimestamp" -> book.lastPriceUpdateTimestamp,
      "signed" -> book.signed,
      "notes" -> book.notes
    )

  implicit object BookFormat extends Format[Book] {

    def reads(json: JsValue): JsResult[Book] = JsSuccess(Book(
      (json \ "_id" \ "$oid").as[Option[String]],
      (json \ "isbn").as[Option[String]],
      (json \ "ean").as[Option[String]],
      (json \ "title").as[String],
      (json \ "authors").as[Option[String]],
      (json \ "binding").as[Option[String]],
      (json \ "edition").as[Option[String]],
      (json \ "numberOfPages").as[Option[String]],
      (json \ "publicationDate").as[Option[String]],
      (json \ "publisher").as[Option[String]],
      (json \ "smallBookCover").as[Option[String]],
      (json \ "largeBookCover").as[Option[String]],
      (json \ "listPrice").as[Option[Int]],
      (json \ "lowestPrice").as[Option[Int]],
      (json \ "totalAvailable").as[Option[Int]],
      (json \ "lastPriceUpdateTimestamp").as[Option[Long]],
      (json \ "signed").as[Boolean],
      (json \ "notes").as[Option[String]]))

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
      "smallBookCover" -> Json.toJson(book.smallBookCover),
      "largeBookCover" -> Json.toJson(book.largeBookCover),
      "listPrice" -> Json.toJson(book.listPrice),
      "lowestPrice" -> Json.toJson(book.lowestPrice),
      "totalAvailable" -> Json.toJson(book.totalAvailable),
      "lastPriceUpdateTimestamp" -> Json.toJson(book.lastPriceUpdateTimestamp),
      "signed" -> Json.toJson(book.signed),
      "notes" -> Json.toJson(book.notes)))

  }

  private def getOptionText(node: NodeSeq): Option[String] =
    node.headOption match {
      case None => None
      case Some(aNode) => Some(aNode.text)
    }

  private def getOptionInt(node: NodeSeq): Option[Int] =
    node.headOption match {
      case None => None
      case Some(aNode) => Some(aNode.text.toInt)
    }

  private def lowestPrice(offerSummaryNode: NodeSeq): Option[Int] = {
    val lowestUsedPrice = getPrice(offerSummaryNode, "LowestUsedPrice")
    val lowestNewPrice = getPrice(offerSummaryNode, "LowestNewPrice")
    lowestUsedPrice match {
      case None => lowestNewPrice
      case Some(uPrice) => lowestNewPrice match {
        case None => lowestUsedPrice
        case Some(nPrice) => if (uPrice <= nPrice) Some(uPrice) else Some(nPrice)
      }
    }
  }

  private def getPrice(offerSummaryNode: NodeSeq, priceName: String): Option[Int] = {
    offerSummaryNode \ priceName \ "Amount" text match {
      case "" => None
      case total => total match {
        case "0" => None
        case amount => Some(amount.toInt)
      }
    }
  }

  def availabilityFromAmazonXml(xml: NodeSeq): Option[OfferSummary] = {
    val offerSummaryNode = xml \ "Items" \ "Item" \ "OfferSummary" head
    val totalUsed = (offerSummaryNode \ "TotalUsed").text
    val totalNew = (offerSummaryNode \ "TotalNew").text
    val totalAvailable = if (totalUsed == "") 0 else totalUsed.toInt + (if (totalNew == "") 0 else totalNew.toInt)
    totalAvailable match {
      case 0 => Some((None, Some(0)))
      case _ => Some((lowestPrice(offerSummaryNode), Some(totalAvailable)))
    }
  }

  def fromAmazonXml(isbn: String, xml: Elem): Option[Book] =
    (xml \\ "Error").size match {
      case 0 => {
        val itemNode = xml \ "Items" \ "Item"
        val itemAttributesNode = itemNode \ "ItemAttributes"
        val authorsString = (itemAttributesNode \ "Author" map (f => f.text) mkString (", "))
        val authors = authorsString match {
          case "" => None
          case _ => Some(authorsString)
        }
        val amazonAvailability = availabilityFromAmazonXml(xml)
        Some(new Book(
          None,
          getOptionText(itemAttributesNode \ "ISBN"),
          getOptionText(itemAttributesNode \ "EAN"),
          itemAttributesNode \ "Title" text,
          authors,
          getOptionText(itemAttributesNode \ "Binding"),
          getOptionText(itemAttributesNode \ "Edition"),
          getOptionText(itemAttributesNode \ "NumberOfPages"),
          getOptionText(itemAttributesNode \ "PublicationDate"),
          getOptionText(itemAttributesNode \ "Publisher"),
          getOptionText(itemNode \ "MediumImage" \ "URL"),
          getOptionText(itemNode \ "LargeImage" \ "URL"),
          getOptionInt(itemAttributesNode \ "ListPrice" \ "Amount"),
          amazonAvailability match {
            case None => None
            case Some(some) => some._1
          },
          amazonAvailability match {
            case None => None
            case Some(some) => some._2
          },
          Some(System.currentTimeMillis()),
          false,
          None))
      }
      case _ => {
        Logger.info("Could not find bookByIsbn: " + isbn)
        None
      }
    }

}