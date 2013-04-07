package model

import xml.{NodeSeq, Elem}
import play.api.libs.json._
import play.api.libs.json.JsObject

case class OfferSummary(
                         lowestUsedPrice: Option[Price],
                         lowestNewPrice: Option[Price],
                         totalUsed: String,
                         totalNew: String
                         ) {

  def displayableLowestUsedPrice: String =
    lowestUsedPrice match {
      case None => "none available, lowest price: ?"
      case Some(price) => totalUsed + " used, lowest price: " + price.toString
    }

  def displayableLowestNewPrice: String =
    lowestNewPrice match {
      case None => "none available, lowest price: ?"
      case Some(price) => totalNew + " new, lowest price: " + price.toString
    }

  override def toString: String = displayableLowestNewPrice + ", " + displayableLowestUsedPrice

  override def equals(other: Any): Boolean =
    other match {
      case that: OfferSummary =>
        (that canEqual this) &&
          lowestUsedPrice == that.lowestUsedPrice &&
          lowestNewPrice == that.lowestNewPrice &&
          totalUsed == that.totalUsed &&
          totalNew == that.totalNew
      case _ => false
    }

  def canEqual(other: Any): Boolean =
    other.isInstanceOf[OfferSummary]

  override def hashCode: Int =
    41 * (
      41 * (
        41 * (
          41 + totalNew.hashCode
          ) + totalUsed.hashCode
        ) + lowestNewPrice.hashCode
      ) + lowestUsedPrice.hashCode()

}

object OfferSummary {

  private def realPrice(priceNode: NodeSeq): Price = {
    Price(
      (priceNode \ "Amount" text) toInt,
      priceNode \ "CurrencyCode" text,
      priceNode \ "FormattedPrice" text)
  }

  private def lowestUsedPrice(offerSummaryNode: NodeSeq, total: String): Option[Price] = {
    getPrice(offerSummaryNode, "LowestUsedPrice", total)
  }

  private def lowestNewPrice(offerSummaryNode: NodeSeq, total: String): Option[Price] = {
    getPrice(offerSummaryNode, "LowestNewPrice", total)
  }

  private def getPrice(offerSummaryNode: NodeSeq, priceName: String, total: String): Option[Price] = {
    offerSummaryNode \ priceName \ "Amount" text match {
      case "" => None
      case _ => total match {
        case "0" => None
        case _ => Some(realPrice(offerSummaryNode \ priceName))
      }
    }
  }

  implicit object OfferSummaryFormat extends Format[Option[OfferSummary]] {

    def reads(json: JsValue): JsResult[Option[OfferSummary]] =
      json match {
        case JsUndefined(_) => JsSuccess(None)
        case JsNull => JsSuccess(None)
        case _ => {
          JsSuccess(Some(OfferSummary(Price.PriceFormat.reads(json \ "lowestUsedPrice").get,
            Price.PriceFormat.reads(json \ "lowestNewPrice").get,
            (json \ "totalUsed").as[String],
            (json \ "totalNew").as[String])))
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
    val firstItem = xml \ "Items" \ "Item" head // sometime multiple items are returned (different languages), just pick the first for the time being
    val offerSummaryNode = firstItem \ "OfferSummary"
    val totalUsed = (offerSummaryNode \ "TotalUsed").text
    val totalNew = (offerSummaryNode \ "TotalNew").text
    Some(OfferSummary(
      lowestUsedPrice(offerSummaryNode, totalUsed),
      lowestNewPrice(offerSummaryNode, totalNew),
      totalUsed,
      totalNew
    ))
  }

}
