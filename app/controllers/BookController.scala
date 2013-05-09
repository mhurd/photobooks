package controllers

import play.api.mvc.Controller
import model.{AmazonBookDataRepositoryComponent, MongoDbBookRepositoryComponent}
import play.api.{Logger, Play}
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

class BookController extends Controller with securesocial.core.SecureSocial {

  def bookRepositoryComponent = BookController.bookRepositoryComponent

  def googleAnalyticsCode = BookController.googleAnalyticsCode

  def adminUserProvider = BookController.adminUserProvider

  def adminUserId = BookController.adminUserId

  def adminUserCheck = BookController.adminUserCheck

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
