package controllers

import play.api.mvc._
import model.Book
import play.api.Logger

import play.api.libs.concurrent.Execution.Implicits._
import concurrent.Future

object BookGetController extends BookController {

  def index = UserAwareAction {
    implicit request =>
      implicit val user = request.user
      val start = System.nanoTime()
      Async {
        bookRepositoryComponent.bookRepository.getBooks().map(res => {
          Logger.info(request.remoteAddress + " - total time to get bookByIsbn index: " + (System.nanoTime() - start) / 1000000 + " milli-seconds")
          Ok(views.html.index(res.filter(book => book.valid), googleAnalyticsCode))
        })
      }
  }

  private def book(f: => String => Future[List[Book]], identifier: String)(implicit request: Request[_], user: Option[securesocial.core.Identity]) = {
    val start = System.nanoTime()
    f(identifier) map (res => {
      res match {
        case Nil => {
          Logger.info(request.remoteAddress + " - 404 not found for books/" + identifier)
          NotFound
        }
        case head :: tail => {
          Logger.info(request.remoteAddress + " - total time to get books/" + identifier + " = " + (System.nanoTime() - start) / 1000000 + " milli-seconds")
          Ok(views.html.book(head, googleAnalyticsCode))
        }
      }
    })
  }

  def bookByIsbn(isbn: String) = UserAwareAction {
    implicit request =>
      implicit val user = request.user
      Async {
        book(bookRepositoryComponent.bookRepository.getBookByIsbn, isbn)
      }
  }

  def bookById(id: String) = UserAwareAction {
    implicit request =>
      implicit val user = request.user
      Async {
        book(bookRepositoryComponent.bookRepository.getBookById, id)
      }
  }

}