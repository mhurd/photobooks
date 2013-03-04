package model


import concurrent.Future


/**
 * http://www.cakesolutions.net/teamblogs/2011/12/19/cake-pattern-in-depth/
 */
trait BookRepositoryComponent {

  def bookRepository: BookRepository

  trait BookRepository {

    def getBooks(): Future[List[Book]]

    def getBook(isbn: String): Future[List[Book]]

    def getOfferSummary(isbn: String): Future[Option[OfferSummary]]

    def updateOfferSummary(book: Book, offerSummary: Option[OfferSummary])

  }

}



