package controllers

import play.api.mvc._
import model.BookRepository

object Application extends Controller {

  def index = Action {
    val start = System.currentTimeMillis()
    Async {
      BookRepository.getBooks() map (res => {
        println("total time: " + (System.currentTimeMillis() - start)/1000 + " seconds")
        Ok(views.html.index(res.filter(book => book.valid)))
      })
    }
  }

  def book(isbn: String) = Action {
    Async {
      BookRepository.getBook(isbn) map (res => Ok(views.html.book(res.head)))
    }
  }

}