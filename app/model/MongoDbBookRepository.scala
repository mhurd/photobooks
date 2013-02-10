package model

import play.api.Play
import com.mongodb.casbah.Imports._
import concurrent.Future
import play.api.cache.Cache
import play.api.libs.json.Json
import play.api.Play.current
import concurrent.ExecutionContext.Implicits.global

class MongoDbBookRepository extends BookRepositoryImpl {

  private val cacheExpiration = 30

  private def getServerAddresses(addresses: String): List[ServerAddress] = {
    addresses match {
      case "" => throw new IllegalArgumentException("No MongoDB Servers configured!")
      case s => {
        val servers = s.split(",")
        (for {
          (host, port) <- servers.map(_.split(":")(0)).zip(servers.map(_.split(":")(1)))
        } yield {
          new ServerAddress(host, port.toInt)
        }).toList
      }
    }
  }

  // property is in the form host:port,host:port
  private val replicaSetServers = getServerAddresses(Play.current.configuration.getString("mongodb.servers").get)
  private val username = Play.current.configuration.getString("mongodb.username").get
  private val password = Play.current.configuration.getString("mongodb.password").get

  println("Connecting to MongoDB servers: " + replicaSetServers)
  val client = MongoConnection(replicaSetServers)

  val photobooksDb = client("photobooks") // the name of the database
  println("Authenticating as user: " + username)
  photobooksDb.authenticate(username, password)

  val booksCollection = photobooksDb("books")

  def getBooks(): Future[List[Book]] = {
    val books = Cache.getOrElse[List[Book]]("books", cacheExpiration) {
      val dbBooks = for {
        bookJson <- booksCollection.find().sort(Map("title" -> 1))
      } yield {
        Book.BookFormat.reads(Json.parse(bookJson.toString)).get
      }
      dbBooks.toList
    }
    Future(books)
  }

  def getBook(isbn: String): Future[List[Book]] = {
    val q = MongoDBObject("isbn" -> isbn)
    val books = Cache.getOrElse[List[Book]](isbn, cacheExpiration) {
      val dbBooks = for {
        bookJson <- booksCollection.find(q)
      } yield {
        Book.BookFormat.reads(Json.parse(bookJson.toString)).get
      }
      dbBooks.toList
    }
    Future(books)
  }

}
