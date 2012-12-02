package controllers

import play.api.mvc._
import amazon.AmazonClient
import xml.{PrettyPrinter, Elem}
import model.Book
import play.api.Play
import play.api.libs.concurrent.{Promise, Akka}
import play.api.Play.current
import java.util.concurrent.TimeUnit

object Application extends Controller {

  private val accessKey = Play.current.configuration.getString("amazon.key.access").get
  private val secretKey = Play.current.configuration.getString("amazon.key.secret").get
  private val associateTag = Play.current.configuration.getString("amazon.associate.tag").get

  private val client = new AmazonClient(accessKey, secretKey, associateTag)

  private val isbns =
    List("0199757143",
      "0224087576",
      "0224089706",
      "0240802772",
      "0253349672",
      "029272649X",
      "029273963X",
      "0300099258",
      "0300126212",
      "0316006939",
      "0316027871",
      "0316117722",
      "0316730254",
      "0321316304",
      "0375422153",
      "0375506209",
      "0385261225",
      "0385503466",
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
      "0752883062",
      "0789306336",
      "0789313812",
      "0809057387",
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
      "1896597858",
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
      "3775726832",
      "3775726837",
      "3775727507",
      "3775729941",
      "3775729949",
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
      "8461483754",
      "8496466809",
      "8496898423",
      "8836614906",
      "8869651657",
      "9070478234",
      "9078909072",
      "9899714909")

  lazy val books = Promise.sequence(isbns map (b => makeBookAsync(b)))

  def makeBookAsync(isbn: String): Promise[Book] = {
    Akka.future[Book] {
      makeBook(isbn)
    }
  }

  def makeBook(isbn: String): Book = {
    println(Thread.currentThread().getName + " - making book: " + isbn)
    val xml = client.findByIsbn(isbn)
    try {
      prettyPrintXml(xml)
      Book.fromXml(xml)
    } catch {
      case nfe: NumberFormatException => {
        prettyPrintXml(xml)
        throw nfe
      }
    }
  }

  private def prettyPrintXml(xml: Elem): String = {
    val pretty = new PrettyPrinter(80, 2)
    pretty.format(xml)
  }

  def index = Action {
    Async {
      books map (res => Ok(views.html.index(res)))
    }
  }

}