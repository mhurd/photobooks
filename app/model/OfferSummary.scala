package model

import xml.{NodeSeq, Elem}

sealed trait OfferSummary {
  def lowestUsedPrice: Price

  def lowestNewPrice: Price

  def totalUsed: String

  def totalNew: String

  override def toString: String = {
    "LOWEST USED PRICE: " + lowestUsedPrice + ", " +
      "LOWEST NEW PRICE: " + lowestNewPrice + ", " +
      "TOTAL USED: " + totalUsed + ", " +
      "TOTAL NEW: " + totalNew
  }

}

private case class KnownOfferSummary(
                                      lowestUsedPrice: Price,
                                      lowestNewPrice: Price,
                                      totalUsed: String,
                                      totalNew: String
                                      ) extends OfferSummary {
}

private case class UnknownOfferSummary() extends OfferSummary {
  def lowestUsedPrice: Price = UnknownPrice()

  def lowestNewPrice: Price = UnknownPrice()

  def totalUsed: String = "Unknown"

  def totalNew: String = "Unknown"
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
    KnownOfferSummary(
      lowestUsedPrice(offerSummaryNode, totalUsed),
      lowestNewPrice(offerSummaryNode, totalNew),
      totalUsed,
      totalNew
    )
  }

}
