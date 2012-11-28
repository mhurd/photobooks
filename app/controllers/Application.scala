package controllers

import play.api.mvc._
import amazon.AmazonClient
import xml.{PrettyPrinter, Elem}
import model.Book
import play.api.Play

object Application extends Controller {

  private val accessKey = Play.current.configuration.getString("amazon.key.access").get
  private val secretKey = Play.current.configuration.getString("amazon.key.secret").get
  private val associateTag = Play.current.configuration.getString("amazon.associate.tag").get

  private val client = new AmazonClient(accessKey, secretKey, associateTag)

  private def prettyPrintXml(xml: Elem): String = {
    val pretty = new PrettyPrinter(80, 2)
    pretty.format(xml)
  }

  def index = Action {
    val xml = client.findByIsbn("9780292739635")
    val book = Book.fromXml(xml)
    Ok(views.html.index(book.toString(), book.bookCovers.get("Large").get.url))
  }

}