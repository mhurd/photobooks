package model

import xml.NodeSeq
import play.api.libs.json._
import play.api.libs.json.JsObject

case class Price(
                  amount: Int,
                  currencyCode: String,
                  formattedPrice: String
                  ) {

  override def toString: String = {
    formattedPrice
  }

}

object Price {

  implicit object PriceFormat extends Format[Option[Price]] {

    def reads(json: JsValue): JsResult[Option[Price]] =
      json match {
        case JsUndefined(_) => JsSuccess(None)
        case JsNull => JsSuccess(None)
        case _ => {
          JsSuccess(Some(Price(
            (json \ "amount").as[Int],
            (json \ "currencyCode").as[String],
            (json \ "formattedPrice").as[String])))
        }
      }

    def writes(priceOption: Option[Price]): JsValue =
      priceOption match {
        case None => JsNull
        case Some(price) => {
          JsObject(List(
            "amount" -> JsNumber(price.amount),
            "currencyCode" -> JsString(price.currencyCode),
            "formattedPrice" -> JsString(price.formattedPrice)))
        }
      }


  }

  def fromAmazonXml(priceNode: NodeSeq): Option[Price] = {
    priceNode.size match {
      case 0 => None
      case 1 => Some(Price(
        (priceNode \ "Amount" text) toInt,
        priceNode \ "CurrencyCode" text,
        priceNode \ "FormattedPrice" text))
      case _ => throw new IllegalArgumentException("Expected 1 price node, found: " + priceNode.size)
    }
  }

}

