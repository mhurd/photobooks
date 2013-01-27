package model

import xml.NodeSeq

sealed trait Price

private case class UnknownPrice() extends Price {

  override def toString: String = {
    "No price found"
  }
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

