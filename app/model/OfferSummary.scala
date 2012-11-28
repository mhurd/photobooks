package model

import xml.Elem

class OfferSummary(
                    val lowestUsedPrice: Price,
                    val lowestNewPrice: Price,
                    val totalUsed: Int,
                    val totalNew: Int
                    ) {

  override def toString(): String = {
    "LOWEST USED PRICE: " + lowestUsedPrice + ", " +
      "LOWEST NEW PRICE: " + lowestNewPrice + ", " +
      "TOTAL USED: " + totalUsed + ", " +
      "TOTAL NEW: " + totalNew
  }

}

object OfferSummary {

  def fromXml(xml: Elem): OfferSummary = {
    val offerSummaryNode = xml \ "Items" \ "Item" \ "OfferSummary"
    val lowestUsedPriceNode = offerSummaryNode \ "LowestUsedPrice"
    val lowestUsedPrice = new Price(
      (lowestUsedPriceNode \ "Amount" text) toInt,
      lowestUsedPriceNode \ "CurrencyCode" text,
      lowestUsedPriceNode \ "FormattedPrice" text
    )
    val lowestNewPriceNode = xml \ "Items" \ "Item" \ "OfferSummary" \ "LowestNewPrice"
    val lowestNewPrice = new Price(
      (lowestNewPriceNode \ "Amount" text) toInt,
      lowestNewPriceNode \ "CurrencyCode" text,
      lowestNewPriceNode \ "FormattedPrice" text
    )
    new OfferSummary(
      lowestUsedPrice,
      lowestNewPrice,
      (offerSummaryNode \ "TotalUsed" text) toInt,
      (offerSummaryNode \ "TotalNew" text) toInt
    )
  }

}
