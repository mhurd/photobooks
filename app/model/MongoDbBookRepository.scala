package model

import play.api.{Logger, Play}
import com.mongodb.casbah.Imports._
import concurrent.Future
import play.api.cache.Cache
import play.api.libs.json.Json
import play.api.Play.current
import concurrent.ExecutionContext.Implicits.global
import org.bson.types.ObjectId

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

  Logger.info("Connecting to MongoDB servers: " + replicaSetServers)
  val client = MongoConnection(replicaSetServers)

  val photobooksDb = client("photobooks") // the name of the database
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


  def findBook(isbn: String): List[Book] = {
    val q = MongoDBObject("isbn" -> isbn)
    Cache.getOrElse[List[Book]](isbn, cacheExpiration) {
      val dbBooks = for {
        bookJson <- booksCollection.find(q)
      } yield {
        //Logger.debug("Getting book: " + bookJson.toString)
        Book.BookFormat.reads(Json.parse(bookJson.toString)).get
      }
      dbBooks.toList
    }
  }

  def getBook(isbn: String): Future[List[Book]] = {
    Future(findBook(isbn))
  }

  def getOfferSummary(isbn: String): Future[Option[OfferSummary]] = {
    findBook(isbn) match {
      case Nil => Future(None)
      case x::xs => Future(x.offerSummary)
    }
  }

  def updateOfferSummary(book: Book, offerSummary: Option[OfferSummary]) {
    val objId = new ObjectId(book.id.get)
    val idObj = DBObject("_id" -> objId)
    try {
      offerSummary match {
        case None => booksCollection.update(idObj, $unset(List("offerSummary")), false, false, WriteConcern.Safe)
        case Some(os) => {
          os.lowestUsedPrice match {
            case None => booksCollection.update(idObj, $unset(List("offerSummary.lowestUsedPrice")), false, false, WriteConcern.Safe)
            case Some(lp) => {
              booksCollection.update(idObj, $set(List(
                "offerSummary.lowestUsedPrice.amount" -> lp.amount,
                "offerSummary.lowestUsedPrice.currencyCode" -> lp.currencyCode,
                "offerSummary.lowestUsedPrice.formattedPrice" -> lp.formattedPrice
              )), false, false, WriteConcern.Safe)
            }
          }
          os.lowestNewPrice match {
            case None => booksCollection.update(idObj, $unset(List("offerSummary.lowestNewPrice")), false, false, WriteConcern.Safe)
            case Some(lp) => {
              booksCollection.update(idObj, $set(List(
                "offerSummary.lowestNewPrice.amount" -> lp.amount,
                "offerSummary.lowestNewPrice.currencyCode" -> lp.currencyCode,
                "offerSummary.lowestNewPrice.formattedPrice" -> lp.formattedPrice
              )), false, false, WriteConcern.Safe)
            }
          }
          booksCollection.update(idObj, $set(List(
            "offerSummary.totalNew" -> os.totalNew,
            "offerSummary.totalUsed" -> os.totalUsed
          )), false, false, WriteConcern.Safe)
          Logger.debug("Updated OfferSummary for '" + book.title + "' with " + os)
        }
      }
    } catch {
      case ex: Exception => {
        Logger.error("Couldn't update OfferSummary for '" + book.title + "' with " + offerSummary, ex)
      }
    }

  }

}
