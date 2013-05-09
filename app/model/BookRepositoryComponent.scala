package model


import concurrent.Future
import model.Book.OfferSummary


/**
 * http://www.cakesolutions.net/teamblogs/2011/12/19/cake-pattern-in-depth/
 */
trait BookRepositoryComponent {

  def bookRepository: BookRepository

  trait BookRepository {

    def getBooks(): Future[List[Book]]

    def getBookByIsbn(isbn: String): Future[List[Book]]

    def getBookById(id: String): Future[List[Book]]

    def getOfferSummary(isbn: String): Future[Option[OfferSummary]]

    def updateOfferSummary(book: Book, offerSummary: Option[OfferSummary])

    def saveBook(book: Book)

  }

}



