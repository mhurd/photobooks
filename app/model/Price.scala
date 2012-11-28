package model

class Price(
             val amount: Int,
             val currencyCode: String,
             val formattedPrice: String
             ) {

  override def toString(): String = {
    formattedPrice
  }

}
