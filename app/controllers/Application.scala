package controllers

import play.api.mvc._
import model.BookRepository
import play.api.Play

object Application extends Controller {

  val bookRepository = BookRepository()
  private val googleAnalyticsCode = Play.current.configuration.getString("google.analytics.code").get


  def index = Action {
    val start = System.currentTimeMillis()
    Async {
      bookRepository.getBooks() map (res => {
        println("total time to get book index: " + (System.currentTimeMillis() - start)/1000 + " seconds")
        Ok(views.html.index(res.filter(book => book.valid), googleAnalyticsCode))
      })
    }
  }

  def book(isbn: String) = Action {
    Async {
      bookRepository.getBook(isbn) map (res => Ok(views.html.book(res.head, googleAnalyticsCode)))
    }
  }

}