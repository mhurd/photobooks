package controllers

import play.api.mvc._
import model.{AmazonBookRepository, BookRepositoryImpl, MongoDbBookRepository, BookRepository}
import play.api.{Logger, Play}

import play.api.libs.concurrent.Execution.Implicits._

object BookGetController extends Controller {

  val books = new BookRepository {
    val repository: BookRepositoryImpl = new MongoDbBookRepository()
  }

  private val googleAnalyticsCode = Play.current.configuration.getString("google.analytics.code").get

  def index = Action {
    request =>
    val start = System.nanoTime()
    Async {
      books.repository.getBooks().map(res => {
        Logger.debug(request.remoteAddress + " - total time to get book index: " + (System.nanoTime() - start) / 1000000 + " milli-seconds")
        Ok(views.html.index(res.filter(book => book.valid), googleAnalyticsCode))
      })
    }
  }

  def book(isbn: String) = Action {
    request =>
    val start = System.nanoTime()
    Async {
      books.repository.getBook(isbn) map (res => {
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