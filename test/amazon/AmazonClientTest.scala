package amazon

import org.scalatest.FlatSpec
import model.{OfferSummary, Book}

class AmazonClientTest extends FlatSpec {


  private val accessKey = sys.props.get("amazon.key.access")
  private val secretKey = sys.props.get("amazon.key.secret")
  private val associateTag = sys.props.get("amazon.associate.tag")

  private val client = AmazonClient(accessKey.get, secretKey.get, associateTag.get)

  val isbn = "0199757143"

  "AmazonClient" must "be able to retrieve a book's details by isbn" in {
    expectResult(isbn) {
      Book.fromAmazonXml(isbn, client.findByIsbn(isbn)).get.isbn.get
    }
  }

  it must "be able to retrieve a book's details by keyword" in {
    expectResult(isbn) {
      Book.fromAmazonXml(isbn, client.findByKeywords(List(isbn))).get.isbn.get
    }
  }

  it must "be able to get offer details for a book by isbn" in {
      expectResult(true) {
        val e = client.findOfferSummaryByIsbn(isbn)
        println(e.toString())
        OfferSummary.fromAmazonXml(e).get.isInstanceOf[OfferSummary]
      }
    }

}
