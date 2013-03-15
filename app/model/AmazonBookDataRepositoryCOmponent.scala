package model

import play.api.{Logger, Play}
import amazon.AmazonClient
import concurrent.{Await, Future}
import akka.actor.{Props, ActorLogging, Actor, ActorSystem}
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import concurrent.duration._
import concurrent.ExecutionContext.Implicits.global

trait AmazonBookDataRepositoryComponent extends BookDataRepositoryComponent {

  this: BookRepositoryComponent =>

  val bookDataRepository: BookDataRepository = new AmazonBookDataRepository

  class AmazonBookDataRepository extends BookDataRepository {

    private val accessKey = Play.current.configuration.getString("amazon.key.access").get
    private val secretKey = Play.current.configuration.getString("amazon.key.secret").get
    private val associateTag = Play.current.configuration.getString("amazon.associate.tag").get

    private val amazonClient = AmazonClient(accessKey, secretKey, associateTag)

    private val isbns =
      List("0199757143",
        "0224087576",
        "0224089706",
        "0253349672",
        "029272649X",
        "029273963X",
        "0300099258",
        "0300126212",
        "0316006939",
        "0316117722",
        "0316730254",
        "0321316304",
        "0375422153",
        "0375506209",
        "0385261225",
        "0393065642",
        "0500512515",
        "0500542783",
        "0500542872",
        "0500543666",
        "0500543992",
        "0500544026",
        "0520204360",
        "0525949852",
        "0679404848",
        "0714844306",
        "0714845736",
        "0714846376",
        "0714846554",
        "0714846643",
        "0714848328",
        "0752226649",
        "0789306336",
        "0789313812",
        "0810945312",
        "081095415X",
        "0810963981",
        "0810993805",
        "0811843181",
        "0811848655",
        "0821221841",
        "0821221868",
        "0821221876",
        "0821228765",
        "0822323559",
        "0847831493",
        "0870703382",
        "0870703781",
        "0870705156",
        "0870707213",
        "087070835X",
        "0893817465",
        "0912810408",
        "091501355X",
        "093511209x",
        "0954281365",
        "0954709128",
        "0955739462",
        "0956887201",
        "0963470701",
        "0974283673",
        "0974886300",
        "0979918839",
        "1426203292",
        "1426206372",
        "1564660567",
        "1567923593",
        "157687429X",
        "1576874478",
        "1597110566",
        "1597110582",
        "1597110612",
        "1597110922",
        "1597110930",
        "1597110949",
        "159711121X",
        "159711135X",
        "1597111449",
        "1597111627",
        "1844003639",
        "1847721109",
        "1847960006",
        "1854379259",
        "1855144174",
        "1861541384",
        "1881337189",
        "1881337200",
        "1881450279",
        "190379630X",
        "1903796326",
        "1903796423",
        "190443844X",
        "1904563724",
        "1904587968",
        "1905712022",
        "190707130X",
        "1907893113",
        "1907946136",
        "1907946144",
        "1931788545",
        "1931885486",
        "1931885516",
        "1931885931",
        "1933045736",
        "1933952474",
        "284426364X",
        "2915173826",
        "2915359385",
        "2916355006",
        "2952410216",
        "2952410224",
        "3775720987",
        "3775726616",
        "3775726837",
        "3775727507",
        "3775729941",
        "3791324845",
        "3791345206",
        "3822856215",
        "3829600461",
        "3836501899",
        "3836503891",
        "383652077X",
        "383652726X",
        "3865211399",
        "3865212336",
        "3865213715",
        "3865214517",
        "386521584X",
        "3865216013",
        "3865216455",
        "3865217168",
        "386521827X",
        "3865219152",
        "386521925X",
        "3865219438",
        "3869302569",
        "3882439602",
        "3931141969",
        "3941825097",
        "490294300X",
        "8496466809",
        "8496898423",
        "8836614906",
        "8869651657",
        "9070478234",
        "9078909072",
        "0870706829",
        "3775731482",
        "0870708120",
        "0957434103",
        "0870700944",
        "190789327X")

    object BookOrdering extends Ordering[Future[Book]] {
      def compare(a: Future[Book], b: Future[Book]) = {
        Await.result(a, 60 seconds).title compare Await.result(b, 60 seconds).title
      }

    }

    val akkaSystem = ActorSystem("PhotoBooksSystem")

    def numberOfAmazonActors(): Int = {
      val parallelismCoefficient = 80 // 1..100, lower for CPU-bound, higher for IO-bound
      val number = Runtime.getRuntime().availableProcessors() * 100 / (100 - parallelismCoefficient)
      Logger.info("Using " + number + " Book Maker actors...")
      number
    }

    sealed trait AmazonMessage

    case class GetBookFromISBN(isbn: String) extends AmazonMessage

    case class GetOfferSummaryFromISBN(isbn: String) extends AmazonMessage

    case class UpdateOfferSummaries() extends AmazonMessage

    case class UpdateOfferSummary(book: Book) extends AmazonMessage

    class AmazonActor extends Actor with ActorLogging {
      def receive = {
        case GetBookFromISBN(isbn) ⇒ {
          try {
            val book = makeBook(isbn)
            if (book.isDefined) {
              sender ! book.get
            }
          } catch {
            case e: Exception ⇒
              sender ! akka.actor.Status.Failure(e)
              throw e
          }
        }
        case GetOfferSummaryFromISBN(isbn) ⇒ {
          try {
            val xml = amazonClient.findOfferSummaryByIsbn(isbn)
            val offerSummary = OfferSummary.fromAmazonXml(xml)
            if (offerSummary.isDefined) {
              sender ! offerSummary
            }
          } catch {
            case e: Exception ⇒
              sender ! akka.actor.Status.Failure(e)
              throw e
          }
        }
        case UpdateOfferSummaries => {
          val booksFuture = bookRepository.getBooks()
          booksFuture.onFailure {
            case ex => Logger.error(ex.getMessage, ex)
          }
          booksFuture.onSuccess {
            case xs => xs map (aBook => {
              if (aBook.isbn.isDefined) (amazonActor ! UpdateOfferSummary(aBook))
            })
          }
        }
        case UpdateOfferSummary(book) => {
          if (book.isbn.isDefined) {
            val osFuture = getOfferSummary(book.isbn.get)
            osFuture.onFailure {
              case ex => Logger.error(ex.getMessage, ex)
            }
            osFuture.onSuccess {
              case os => {
                if (book.offerSummary != os) bookRepository.updateOfferSummary(book, os)
                else Logger.debug("Did not update OfferSummary for '" + book.title + "' it hasn't changed")
              }
            }
          }
        }
      }
    }

    val amazonActor = akkaSystem.actorOf(
      Props[AmazonActor].withCreator(new AmazonActor()).withRouter(RoundRobinRouter(numberOfAmazonActors())), name = "bookMakerRouter")

    val scheduledUpdate =
      akkaSystem.scheduler.schedule(30 minutes,
        8 hours,
        amazonActor,
        UpdateOfferSummaries)

    sys.addShutdownHook({
      Logger.info("Shutting down akka...")
      scheduledUpdate.cancel
      akkaSystem.shutdown
    })

    lazy private val books = makeBooks(isbns)

    private def makeBooks(isbns: List[String]): Future[List[Book]] = {
      implicit val timeout = Timeout(60, TimeUnit.SECONDS)
      Future.sequence(isbns.map(isbn => {
        (amazonActor ? GetBookFromISBN(isbn)).mapTo[Book]
      }).sorted(BookOrdering))
    }

    private def makeBook(isbn: String): Option[Book] = {
      Logger.debug(Thread.currentThread().getName + " - looking up bookByIsbn: " + isbn)
      val xml = amazonClient.findByIsbn(isbn)
      try {
        val bookOption = Book.fromAmazonXml(isbn, xml)
        // bookOption match {
        //   case Some(bookByIsbn) => println (Book.BookFormat.writes(bookByIsbn))
        //   case None => println("")
        //}
        bookOption
      } catch {
        case nfe: NumberFormatException => {
          throw nfe
        }
      }
    }

    def getBooks(): Future[List[Book]] = {
      books
    }

    def getBook(isbn: String): Future[List[Book]] = {
      makeBooks(List(isbn))
    }

    def getOfferSummary(isbn: String): Future[Option[OfferSummary]] = {
      implicit val timeout = Timeout(60, TimeUnit.SECONDS)
      (amazonActor ? GetOfferSummaryFromISBN(isbn)).mapTo[Option[OfferSummary]]
    }
  }

}
