package model

import xml.{NodeSeq, Elem}

case class OfferSummary(
                         lowestUsedPrice: Price,
                         lowestNewPrice: Price,
                         totalUsed: String,
                         totalNew: String
                         ) {

  override def toString: String = {
    "LOWEST USED PRICE: " + lowestUsedPrice + ", " +
      "LOWEST NEW PRICE: " + lowestNewPrice + ", " +
      "TOTAL USED: " + totalUsed + ", " +
      "TOTAL NEW: " + totalNew
  }

}

object OfferSummary {

  private def realPrice(priceNode: NodeSeq): Price = {
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

  private def lowestUsedPrice(offerSummaryNode: NodeSeq, total: String): Price = {
    total match {
      case "0" => UnknownPrice()
      case _ => realPrice(offerSummaryNode \ "LowestUsedPrice")
    }
  }

  private def lowestNewPrice(offerSummaryNode: NodeSeq, total: String): Price = {
    total match {
      case "0" => UnknownPrice()
      case _ => realPrice(offerSummaryNode \ "LowestNewPrice")
    }
  }

  def fromXml(xml: Elem): OfferSummary = {
    val offerSummaryNode = xml \ "Items" \ "Item" \ "OfferSummary"
    val totalUsed = (offerSummaryNode \ "TotalUsed").text
    val totalNew = (offerSummaryNode \ "TotalNew").text
    OfferSummary(
      lowestUsedPrice(offerSummaryNode, totalUsed),
      lowestNewPrice(offerSummaryNode, totalNew),
      totalUsed,
      totalNew
    )
  }

}
