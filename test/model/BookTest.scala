package model

import org.scalatest.FlatSpec
import play.api.libs.json.Json

class BookTest extends FlatSpec {

  val book = Book(
              None,
              Some("an isbn"),
              Some("my ean"),
              "a title",
              Some("me and my friend"),
              Some("Hardcover"),
              Some("First Edition"),
              Some("100"),
              Some("03-01-1977"),
              Some("ACME"),
              Some("http://somewhere.com/small.jpg"),
              Some("http://somewhere.com/large.jpg"),
              Some(1223),
              Some(1111),
              Some(10),
              Some(System.currentTimeMillis()),
              true,
              Some("some boring musings"))

  "BookFormat" must "be able to 'read' a JSON book from the output of 'writing' out JSON" in {
    expectResult(true) {
      Book.BookFormat.reads(Book.BookFormat.writes(book)).get.isbn.get == book
    }
  }

  it must "be able to 'read' another JSON book from the output of 'writing' out JSON" in {
    expectResult(true) {
      Book.BookFormat.reads(Book.BookFormat.writes(book)).get.isbn.get == book
    }
  }

}
