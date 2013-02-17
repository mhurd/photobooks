package controllers

import play.api.data._
import play.api.data.Forms._
import model.BookCover
import model.Price
import model.Book
import model.OfferSummary

object BookEditController {

  val bookCoverMapping = mapping(
    "url" -> text
  )(BookCover.apply)(BookCover.unapply)

  val priceMapping = mapping(
    "amount" -> number,
    "currencyCode" -> text,
    "formattedPrice" -> text
  )(Price.apply)(Price.unapply)

  val offerSummaryMapping = mapping(
    "lowestUsedPrice" -> optional(priceMapping),
    "lowestNewPrice" -> optional(priceMapping),
    "totalUsed" -> text,
    "totalNew" -> text
  )(OfferSummary.apply)(OfferSummary.unapply)

  val bookForm = Form(
    mapping(
      "isbn" -> optional(text),
      "ean" -> optional(text),
      "title" -> text,
      "authors" -> optional(text),
      "binding" -> optional(text),
      "edition" -> optional(text),
      "numberOfPages" -> optional(text),
      "publicationDate" -> optional(text),
      "publisher" -> optional(text),
      "bookCover" -> bookCoverMapping,
      "listPrice" -> optional(priceMapping),
      "offerSummary" -> optional(offerSummaryMapping)
    )(Book.apply)(Book.unapply)
  )

}
