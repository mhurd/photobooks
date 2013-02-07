package model

import org.scalatest.FlatSpec
import play.api.libs.json.Json

class BookTest extends FlatSpec {

  val jsonComplete = """{"isbn":"0199757143","ean":"9780199757145","title":"Philosophers","authors":"Steve Pyke","binding":"Hardcover","numberOfPages":"224","publicationDate":"2011-08-25","publisher":"OUP USA","bookCover":{"url":"http://ecx.images-amazon.com/images/I/51-nWfH1u2L.jpg"},"listPrice":{"amount":2500,"currencyCode":"GBP","formattedPrice":"£25.00"},"offerSummary":{"lowestUsedPrice":{"amount":1449,"currencyCode":"GBP","formattedPrice":"£14.49"},"lowestNewPrice":{"amount":1368,"currencyCode":"GBP","formattedPrice":"£13.68"},"totalUsed":"11","totalNew":"21"}}"""
  val jsonNoIsbnOrEan = """{"title":"Philosophers","authors":"Steve Pyke","binding":"Hardcover","numberOfPages":"224","publicationDate":"2011-08-25","publisher":"OUP USA","bookCover":{"url":"http://ecx.images-amazon.com/images/I/51-nWfH1u2L.jpg"},"listPrice":{"amount":2500,"currencyCode":"GBP","formattedPrice":"£25.00"},"offerSummary":{"lowestUsedPrice":{"amount":1449,"currencyCode":"GBP","formattedPrice":"£14.49"},"lowestNewPrice":{"amount":1368,"currencyCode":"GBP","formattedPrice":"£13.68"},"totalUsed":"11","totalNew":"21"}}"""
  val jsonNoListPrice = """{"isbn":"0199757143","ean":"9780199757145","title":"Philosophers","authors":"Steve Pyke","binding":"Hardcover","numberOfPages":"224","publicationDate":"2011-08-25","publisher":"OUP USA","bookCover":{"url":"http://ecx.images-amazon.com/images/I/51-nWfH1u2L.jpg"},"offerSummary":{"lowestUsedPrice":{"amount":1449,"currencyCode":"GBP","formattedPrice":"£14.49"},"lowestNewPrice":{"amount":1368,"currencyCode":"GBP","formattedPrice":"£13.68"},"totalUsed":"11","totalNew":"21"}}"""

  "BookFormat" must "be able to construct a Book from complete JSON" in {
    expectResult("0199757143") {
      Book.BookFormat.reads(Json.parse(jsonComplete)).get.isbn.get
    }
  }

  it must "be able to construct a Book from incomplete JSON missing an ISBN or EAN" in {
    expectResult("None") {
      Book.BookFormat.reads(Json.parse(jsonNoIsbnOrEan)).get.isbn.toString
    }
  }

  it must "be able to construct a Book from incomplete JSON missing a List Price" in {
    expectResult("None") {
      Book.BookFormat.reads(Json.parse(jsonNoListPrice)).get.listPrice.toString
    }
  }

}
