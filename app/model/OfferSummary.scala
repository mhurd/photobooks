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
}

object OfferSummary {

  private def realPrice(priceNode: NodeSeq): Price = {
    Price(
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
    val offerSummaryNode = xml \ "Items" \ "Item" \ "OfferSummary"
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
