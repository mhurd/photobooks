package controllers

import play.api.mvc._
import model.{AmazonBookDataRepositoryComponent, MongoDbBookRepositoryComponent}
import play.api.{Logger, Play}

import play.api.libs.concurrent.Execution.Implicits._

object BookGetController extends Controller with securesocial.core.SecureSocial {

  val bookRepositoryComponent = new MongoDbBookRepositoryComponent {}
  val bookDataRepo = new AmazonBookDataRepositoryComponent() with MongoDbBookRepositoryComponent

  private val googleAnalyticsCode = Play.current.configuration.getString("google.analytics.code").get

  def index = UserAwareAction {
    implicit request =>
    implicit val user = request.user
    val start = System.nanoTime()
    Async {
      bookRepositoryComponent.bookRepository.getBooks().map(res => {
        Logger.debug(request.remoteAddress + " - total time to get book index: " + (System.nanoTime() - start) / 1000000 + " milli-seconds")
        Ok(views.html.index(res.filter(book => book.valid), googleAnalyticsCode))
      })
    }
  }

  def book(isbn: String) = UserAwareAction {
    implicit request =>
    implicit val user = request.user
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
            Ok(views.html.book(head, googleAnalyticsCode))
          }
        }
      })
    }
  }

}