package model

sealed trait Price

case class UnknownPrice() extends Price {

  override def toString: String = {
    "No price found"
  }
}

case class KnownPrice(
                       amount: Int,
                       currencyCode: String,
                       formattedPrice: String
                       ) extends Price {

  override def toString: String = {
    formattedPrice
  }

}
