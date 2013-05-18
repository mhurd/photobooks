package controllers

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.data.Form
import play.api.data.Forms._
import scala.Some

object LookupBookController extends BookController {

  case class FindBook(isbn: String)

  val lookupBookForm = Form(
        mapping(
          "isbn" -> text
        )(FindBook.apply)(FindBook.unapply)
      )

  def submit() = SecuredAction(adminUserCheck) {
    implicit request =>
      implicit val user = Some(request.user)
      lookupBookForm.bindFromRequest.fold(
        formWithErrors => {
          Ok(views.html.lookupBook(formWithErrors, googleAnalyticsCode))
        },
        form => {
          Logger.info("Finding book: " + form.isbn)
          val book = bookRepositoryComponent.bookDataRepository.getBook(form.isbn)
          book map (books =>
            books match {
              case Nil => NotFound
              case book :: _ => {
                val filledForm = bookForm.fill(book)
                Ok(views.html.bookEdit(filledForm, googleAnalyticsCode))
              }
            }
          )
          NotFound
        }
      )
  }

  def lookupBook() = SecuredAction(adminUserCheck) {
    implicit request =>
    implicit val user = Some(request.user)
    Ok(views.html.lookupBook(lookupBookForm.discardingErrors, googleAnalyticsCode))
  }

}
