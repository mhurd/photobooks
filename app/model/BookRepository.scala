package model


import concurrent.Future


/**
 * @see <a href="http://blog.rintcius.nl/post/di-on-steroids-with-scala-value-injection-on-traits.html">DI in Scala</a>
 */
trait BookRepository {

  val repository: BookRepositoryImpl

}

trait BookRepositoryImpl {

  def getBooks(): Future[List[Book]]

  def getBook(isbn: String): Future[List[Book]]

}




