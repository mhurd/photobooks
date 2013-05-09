package model

import concurrent.Future
import model.Book.OfferSummary

/**
 * http://www.cakesolutions.net/teamblogs/2011/12/19/cake-pattern-in-depth/
 */
trait BookDataRepositoryComponent {

  def bookDataRepository: BookDataRepository

  trait BookDataRepository {

    def getBooks(): Future[List[Book]]

    def getBook(isbn: String): Future[List[Book]]

    def getOfferSummary(isbn: String): Future[Option[OfferSummary]]

  }

}

