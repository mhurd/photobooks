package controllers

import play.api.mvc.Request
import model._
import play.api.Logger

import play.api.data.Form
import play.api.data.Forms._

import play.api.libs.concurrent.Execution.Implicits._
import concurrent.Future

object BookEditController extends BookController {

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

  def submitWithId(id: String) = SecuredAction(adminUserCheck) {
    implicit request =>
      implicit val user = Some(request.user)
      Logger.info("Submitted " + id)
      bookForm.bindFromRequest.fold(
        formWithErrors => {
          Logger.info("Form with errors: " + id)
          Ok(views.html.bookEdit(formWithErrors, googleAnalyticsCode))
        },
        book => {
          bookRepositoryComponent.bookRepository.updateBook(book)
          Redirect(routes.BookGetController.bookById(book.id.get))
        }
      )
  }

  private def editBook(f: String => Future[List[Book]], identifier: String)(implicit request: Request[_], user: Option[securesocial.core.Identity]) = {
    val start = System.nanoTime()
    f(identifier) map (res => {
      res match {
        case Nil => {
          Logger.info(request.remoteAddress + " - 404 not found for books/" + identifier)
          NotFound
        }
        case head :: tail => {
          Logger.info(request.remoteAddress + " - total time to get books/" + identifier + " = " + (System.nanoTime() - start) / 1000000 + " milli-seconds")
          val filledForm = bookForm.fill(head)
          Ok(views.html.bookEdit(filledForm, googleAnalyticsCode))
        }
      }
    })
  }

  def editBookByIsbn(isbn: String) = SecuredAction(adminUserCheck) {
    implicit request =>
      implicit val user = Some(request.user)
      Logger.info("editBookByIsbn by user: " + request.user.fullName + " provider: " + request.user.id.providerId + " id: " + request.user.id.id)
      Async {
        editBook(bookRepositoryComponent.bookRepository.getBookByIsbn, isbn)
      }
  }

  def editBookById(id: String) = SecuredAction(adminUserCheck) {
    implicit request =>
      implicit val user = Some(request.user)
      Logger.info("editBookById by user: " + request.user.fullName + " provider: " + request.user.id.providerId + " id: " + request.user.id.id)
      Async {
        editBook(bookRepositoryComponent.bookRepository.getBookById, id)
      }
  }

}
