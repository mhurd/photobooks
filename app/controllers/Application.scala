package controllers

import play.api.mvc._
import model.BookRepository

object Application extends Controller {

  def index = Action {
    Async {
      BookRepository.books map (res => Ok(views.html.index(res.filter(book => book.valid))))
    }
  }

  def book(isbn: String) = Action {
    Async {
      BookRepository.makeBookAsync(isbn) map (res => Ok(views.html.book(res)))
    }
  }

}