package controllers

import play.api.mvc.Controller
import model.{Book, AmazonBookDataRepositoryComponent, MongoDbBookRepositoryComponent}
import play.api.{Logger, Play}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.api.data.Form
import play.api.data.Forms._

class BookController extends Controller with securesocial.core.SecureSocial {

  def bookRepositoryComponent = BookController.bookRepositoryComponent

  def googleAnalyticsCode = BookController.googleAnalyticsCode

  def adminUserProvider = BookController.adminUserProvider

  def adminUserId = BookController.adminUserId

  def adminUserCheck = BookController.adminUserCheck

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
      "smallBookCover" -> optional(text),
      "largeBookCover" -> optional(text),
      "listPrice" -> optional(number),
      "lowestPrice" -> optional(number),
      "totalAvailable" -> optional(number),
      "lastPriceUpdateTimestamp" -> optional(longNumber()),
      "amazonPageUrl" -> optional(text)
    )(Book.apply)(Book.unapply)
  )

}

private object BookController {

  val bookRepositoryComponent = new MongoDbBookRepositoryComponent with AmazonBookDataRepositoryComponent {}

  val googleAnalyticsCode = Play.current.configuration.getString("google.analytics.code").get

  val adminUserProvider = Play.current.configuration.getString("admin.user.provider").get
  val adminUserId = Play.current.configuration.getString("admin.user.id").get

  val adminUserCheck = WithProviderAndUserId(adminUserProvider, adminUserId)

  bookRepositoryComponent.bookRepository.getBooks() map (res => res match {
    case Nil => {
      Logger.info("No books found in the database, loading the static data from Amazon...")
      bookRepositoryComponent.bookDataRepository.getBooks().map(_ map (bookRepositoryComponent.bookRepository.saveBook(_)))
    }
    case _ =>
  })

}
