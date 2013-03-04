package controllers

import play.api.mvc.Results.{Async, NotFound, Ok}
import play.api.mvc.Action
import model._
import play.api.{Play, Logger}

import play.api.data.Form
import play.api.data.Forms._

import play.api.libs.concurrent.Execution.Implicits._

object BookEditController {

  val bookRepositoryComponent = new MongoDbBookRepositoryComponent {}

  private val googleAnalyticsCode = Play.current.configuration.getString("google.analytics.code").get

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
      "id" -> optional(text),
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

  def submit(isbn: String) = Action {
    implicit request =>
      Logger.debug("Submitted " + isbn)
      bookForm.bindFromRequest.fold(
        formWithErrors => {
          Logger.debug("Form with errors: " + isbn)
          Ok(views.html.bookEdit(formWithErrors, googleAnalyticsCode))
        },
        value => Ok("created: " + value)
      )
  }

  def createBook() = Action {
    Ok(views.html.bookEdit(bookForm, googleAnalyticsCode))
  }

  def editBook(isbn: String) = Action {
    request =>
      val start = System.nanoTime()
      Async {
        bookRepositoryComponent.bookRepository.getBook(isbn) map (res => {
          res match {
            case Nil => {
              Logger.debug(request.remoteAddress + " - 404 not found for books/" + isbn)
              NotFound
            }
            case head :: tail => {
              Logger.debug(request.remoteAddress + " - total time to get books/" + isbn + " = " + (System.nanoTime() - start) / 1000000 + " milli-seconds")
              val filledForm = bookForm.fill(head)
              Ok(views.html.bookEdit(filledForm, googleAnalyticsCode))
            }
          }
        })
      }
  }

}
