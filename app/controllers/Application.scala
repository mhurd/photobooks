package controllers

import play.api.mvc._
import model.{AmazonBookRepository, BookRepositoryImpl, MongoDbBookRepository, BookRepository}
import play.api.Play

import play.api.libs.concurrent.Execution.Implicits._

object Application extends Controller {

  val books = new BookRepository {
    val repository: BookRepositoryImpl = new MongoDbBookRepository()
  }

  private val googleAnalyticsCode = Play.current.configuration.getString("google.analytics.code").get

  def index = Action {
    val start = System.nanoTime()
    Async {
      books.repository.getBooks().map(res => {
        println("total time to get book index: " + (System.nanoTime() - start) / 1000000 + " milli-seconds")
        Ok(views.html.index(res.filter(book => book.valid), googleAnalyticsCode))
      })
    }
  }

  def book(isbn: String) = Action {
    val start = System.nanoTime()
    Async {
      books.repository.getBook(isbn) map (res => {
        println("total time to get book page: " + (System.nanoTime() - start) / 1000000 + " milli-seconds")
        Ok(views.html.book(res.head, googleAnalyticsCode))
      })
    }
  }

}