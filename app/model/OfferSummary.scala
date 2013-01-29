package model

import xml.{NodeSeq, Elem}
import play.api.libs.json._
import play.api.libs.json.JsObject

sealed trait OfferSummary {
  def lowestUsedPrice: Option[Price]

  def lowestNewPrice: Option[Price]

  def totalUsed: String

  def totalNew: String

  override def toString: String = {
    "LOWEST USED PRICE: " + lowestUsedPrice + ", " +
      "LOWEST NEW PRICE: " + lowestNewPrice + ", " +
      "TOTAL USED: " + totalUsed + ", " +
      "TOTAL NEW: " + totalNew
  }

}

private case class OfferSummaryImpl(
                                     lowestUsedPrice: Option[Price],
                                     lowestNewPrice: Option[Price],
                                     totalUsed: String,
                                     totalNew: String
                                     ) extends OfferSummary {
}

object OfferSummary {

  private def realPrice(priceNode: NodeSeq): Price = {
    PriceImpl(
      (priceNode \ "Amount" text) toInt,
      priceNode \ "CurrencyCode" text,
      priceNode \ "FormattedPrice" text)
  }

  private def lowestUsedPrice(offerSummaryNode: NodeSeq, total: String): Option[Price] = {
    total match {
      case "0" => None
      case _ => Some(realPrice(offerSummaryNode \ "LowestUsedPrice"))
    }
  }

  private def lowestNewPrice(offerSummaryNode: NodeSeq, total: String): Option[Price] = {
    total match {
      case "0" => None
      case _ => Some(realPrice(offerSummaryNode \ "LowestNewPrice"))
    }
  }

  implicit object OfferSummaryFormat extends Format[Option[OfferSummary]] {

    def reads(json: JsValue): Option[OfferSummary] =
      json match {
        case JsUndefined(_) => None
        case JsNull => None
        case _ => {
          Some(OfferSummaryImpl(Price.PriceFormat.reads(json \ "lowestUsedPrice"),
            Price.PriceFormat.reads(json \ "lowestNewPrice"),
            (json \ "totalUsed").as[String],
            (json \ "totalNew").as[String]))
        }
      }

    def writes(offerSummaryOption: Option[OfferSummary]): JsValue =
      offerSummaryOption match {
        case None => JsNull
        case Some(offerSummary) => {
          JsObject(List(
            "lowestUsedPrice" -> Price.PriceFormat.writes(offerSummary.lowestUsedPrice),
            "lowestNewPrice" -> Price.PriceFormat.writes(offerSummary.lowestNewPrice),
            "totalUsed" -> JsString(offerSummary.totalUsed),
            "totalNew" -> JsString(offerSummary.totalNew)))
        }
      }

  }

  def fromAmazonXml(xml: Elem): Option[OfferSummary] = {
    val offerSummaryNode = xml \ "Items" \ "Item" \ "OfferSummary"
    val totalUsed = (offerSummaryNode \ "TotalUsed").text
    val totalNew = (offerSummaryNode \ "TotalNew").text
    Some(OfferSummaryImpl(
      lowestUsedPrice(offerSummaryNode, totalUsed),
      lowestNewPrice(offerSummaryNode, totalNew),
      totalUsed,
      totalNew
    ))
  }

}
