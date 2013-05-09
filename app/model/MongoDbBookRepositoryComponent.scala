package model

import play.api.{Logger, Play}
import com.mongodb.casbah.Imports._
import concurrent.Future
import play.api.cache.Cache
import play.api.libs.json.Json
import play.api.Play.current
import concurrent.ExecutionContext.Implicits.global
import org.bson.types.ObjectId
import model.Book.OfferSummary

trait MongoDbBookRepositoryComponent extends BookRepositoryComponent {

  val bookRepository: BookRepository = new MongoDbBookRepository

  class MongoDbBookRepository extends BookRepository {

    private val cacheExpiration = 5

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
    private val database = Play.current.configuration.getString("mongodb.database").get

    Logger.info("Connecting to MongoDB servers: " + replicaSetServers)
    val client = MongoConnection(replicaSetServers)

    val photobooksDb = client(database) // the name of the database
    Logger.info("Authenticating as user: " + username)
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

    private def findBooks(identifier: String, query: DBObject): List[Book] = {
      Cache.getOrElse[List[Book]](identifier, cacheExpiration) {
        val dbBooks = for {
          bookJson <- booksCollection.find(query)
        } yield {
          Book.BookFormat.reads(Json.parse(bookJson.toString)).get
        }
        dbBooks.toList
      }
    }

    private def findBookByIsbn(isbn: String): List[Book] = {
      val q = MongoDBObject("isbn" -> isbn)
      findBooks(isbn, q)
    }

    private def findBookById(id: String): List[Book] = {
      val objId = new ObjectId(id)
      val idObj = DBObject("_id" -> objId)
      findBooks(id, idObj)
    }

    def getBookByIsbn(isbn: String): Future[List[Book]] = {
      Future(findBookByIsbn(isbn))
    }

    def getBookById(id: String): Future[List[Book]] = {
      Future(findBookById(id))
    }

    def getOfferSummary(isbn: String): Future[Option[OfferSummary]] = {
      findBookByIsbn(isbn) match {
        case Nil => Future(None)
        case x :: xs => Future(Some((x.lowestPrice, x.totalAvailable)))
      }
    }

    def saveBook(book: Book) {
      try {
        booksCollection.save(book, WriteConcern.Safe)
        Logger.info("Saved book: " + book.title)
      } catch {
        case ex: Exception => {
          Logger.error("Couldn't persist book '" + book.title + "'", ex)
        }
      }
    }

    def updateOfferSummary(book: Book, offerSummary: Option[OfferSummary]) {
      val objId = new ObjectId(book.id.get)
      val idObj = DBObject("_id" -> objId)
      try {
        offerSummary match {
          case None => {
            booksCollection.update(idObj, $set(List(
                              "totalAvailable" -> 0
                            )), false, false, WriteConcern.Safe)
          }
          case Some(os) => {
            os._1 match {
              case None => {
                booksCollection.update(idObj, $set(List(
                  "totalAvailable" -> 0
                )), false, false, WriteConcern.Safe)
              }
              case Some(lp) => {
                booksCollection.update(idObj, $set(List(
                  "lowestPrice" -> lp,
                  "totalAvailable" -> os._2,
                  "lowestPriceModifiedDate" -> System.currentTimeMillis()
                )), false, false, WriteConcern.Safe)
              }
            }
            Logger.info("Updated book pricing details for '" + book.title + "' with " + os)
          }
        }
      } catch {
        case ex: Exception => {
          Logger.error("Couldn't update book pricing details for '" + book.title + "' with " + offerSummary, ex)
        }
      }

    }
  }

}
