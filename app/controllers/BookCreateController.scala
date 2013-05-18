package controllers

import play.api.Logger

object BookCreateController extends BookController {

  def submit() = SecuredAction(adminUserCheck) {
    implicit request =>
      implicit val user = Some(request.user)
      bookForm.bindFromRequest.fold(
        formWithErrors => {
          Ok(views.html.bookEdit(formWithErrors, googleAnalyticsCode))
        },
        book => {
          Logger.info("Submitted new book: " + book.title)
          bookRepositoryComponent.bookRepository.saveBook(book)
          Redirect(routes.BookGetController.bookById(book.id.get))
        }
      )
  }

  def createBook() = SecuredAction(adminUserCheck) {
      implicit request =>
      implicit val user = Some(request.user)
      Ok(views.html.bookCreate(bookForm.discardingErrors, googleAnalyticsCode))
    }

}
