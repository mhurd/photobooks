package model

import xml.NodeSeq
import play.api.libs.json._
import play.api.libs.json.JsObject

sealed trait Price {

  def amount: Int

  def currencyCode: String

  def formattedPrice: String

}

private case class UnknownPrice() extends Price {

  override def toString: String = {
    "No price found"
  }

  def amount: Int = 0

  def currencyCode: String = "Not set"

  def formattedPrice: String = "Not set"

}

private case class KnownPrice(
                               amount: Int,
                               currencyCode: String,
                               formattedPrice: String
                               ) extends Price {

  override def toString: String = {
    formattedPrice
  }

}

object Price {

  implicit object PriceFormat extends Format[Price] {

    def reads(json: JsValue): Price = KnownPrice(
      (json \ "amount").as[Int],
      (json \ "currencyCode").as[String],
      (json \ "formattedPrice").as[String]
    )

    def writes(price: Price): JsValue = JsObject(List(
      "amount" -> JsNumber(price.amount),
      "currencyCode" -> JsString(price.currencyCode),
      "formattedPrice" -> JsString(price.formattedPrice)))

  }

  def fromAmazonXml(priceNode: NodeSeq): Price = {
    priceNode.size match {
      case 0 => UnknownPrice()
      case 1 => KnownPrice(
        (priceNode \ "Amount" text) toInt,
        priceNode \ "CurrencyCode" text,
        priceNode \ "FormattedPrice" text
      )
      case _ => throw new IllegalArgumentException("Expected 1 price node, found: " + priceNode.size)
    }
  }

}

