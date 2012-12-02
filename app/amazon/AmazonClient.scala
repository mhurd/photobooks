package amazon

import org.jboss.netty.handler.codec.http._
import com.twitter.finagle.Service
import com.twitter.finagle.builder.ClientBuilder
import com.twitter.finagle.http.Http
import com.twitter.util.{Duration, Future}
import scala.collection.immutable.SortedMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.net.{InetSocketAddress, URLEncoder}
import xml.Elem
import scala.Predef._

case class AmazonClient(private val accessKey: String, private val secretKey: String, private val associateTag: String) {

  // Amazon API Constants
  private val AMAZON_API_VERSION = "2011-08-01"
  private val AMAZON_SERVICE = "AWSECommerceService"
  private val AMAZON_API_HOST = "ecs.amazonaws.co.uk"
  private val AMAZON_API_REQUEST_URI = "/onca/xml"

  // Request signing
  private val SHA_256 = "HmacSHA256"
  private val UTF8_CHARSET = "UTF-8"
  private val encoder = new Base64(0)
  private val ISO_8601_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
  private val secretKeySpec: SecretKeySpec = new SecretKeySpec(secretKey.getBytes(UTF8_CHARSET), SHA_256)
  private val mac = Mac.getInstance(SHA_256)
  mac.init(secretKeySpec)

  // HTTP client constants
  private val DEFAULT_TCP_TIMEOUT = Duration.fromTimeUnit(10, TimeUnit.SECONDS)
  private val DEFAULT_TIMEOUT = Duration.fromTimeUnit(10, TimeUnit.SECONDS)
  private val HOST_CONNECTION_LIMIT = 1
  private val DEFAULT_HTTP_PORT = 80
  private val MAX_RETRYS = 3

  // Base arguments for the Amazon API request
  private val BASIC_ARGUMENTS = SortedMap(
    "Service" -> AMAZON_SERVICE,
    "Version" -> AMAZON_API_VERSION,
    "AWSAccessKeyId" -> accessKey,
    "AssociateTag" -> associateTag,
    "SearchIndex" -> "Books",
    "Condition" -> "All",
    "Offer" -> "All",
    "ResponseGroup" -> "ItemAttributes,OfferSummary,Images"
  )

  private val httpClient: Service[HttpRequest, HttpResponse] = ClientBuilder()
    .codec(Http())
    .hosts(new InetSocketAddress(AMAZON_API_HOST, DEFAULT_HTTP_PORT))
    .hostConnectionLimit(HOST_CONNECTION_LIMIT)
    .tcpConnectTimeout(DEFAULT_TCP_TIMEOUT)
    .build()

  private def getIso8601TimestampString: String = {
    val format = new java.text.SimpleDateFormat(ISO_8601_TIMESTAMP_FORMAT)
    format.format(new java.util.Date())
  }

  private def percentEncodeRfc3986(s: String): String = {
    URLEncoder.encode(s, UTF8_CHARSET).replace("+", "%20")
  }

  private def hmac(stringToSign: String): String = {
    val data = stringToSign.getBytes(UTF8_CHARSET)
    val rawHmac = mac.doFinal(data)
    new String(encoder.encode(rawHmac))
  }

  private def mergeArguments(arguments: SortedMap[String, String]): String = {
    val mergedArguments: SortedMap[String, String] = BASIC_ARGUMENTS ++ arguments + (("Timestamp" -> getIso8601TimestampString))
    val f = {
      (p: (String, String)) => percentEncodeRfc3986(p._1) + "=" + percentEncodeRfc3986(p._2)
    }
    mergedArguments.map(f).mkString("&")
  }

  private def getSignedUrl(arguments: SortedMap[String, String]): String = {
    val args = mergeArguments(arguments)
    val toSign = "GET" + "\n" + AMAZON_API_HOST + "\n" + AMAZON_API_REQUEST_URI + "\n" + args
    val hmacResult = hmac(toSign)
    val sig = percentEncodeRfc3986(hmacResult)
    AMAZON_API_REQUEST_URI + "?" + args + "&Signature=" + sig
  }

  private def find(arguments: SortedMap[String, String], timeout: Duration, retryCount: Int): Elem = {
    val request: HttpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, getSignedUrl(arguments))
    request.addHeader("Host", AMAZON_API_HOST)
    val responseFuture: Future[HttpResponse] = httpClient(request)
    responseFuture.apply(timeout)
    responseFuture.get().getStatus match {
      case HttpResponseStatus.OK => scala.xml.XML.loadString(responseFuture.get().getContent.toString(Charset.forName(UTF8_CHARSET)))
      case _ => {
        if (retryCount < MAX_RETRYS)
          find(arguments, timeout, retryCount+1)
        else
          throw new RuntimeException(responseFuture.get().getContent.toString(Charset.forName(UTF8_CHARSET)))
      }
    }
  }

  def findByKeywords(keywords: List[String]): Elem = {
    find(SortedMap("Operation" -> "ItemSearch", "Keywords" -> keywords.mkString("+")), DEFAULT_TIMEOUT, 0)
  }

  def findByIsbn(isbn: String): Elem = {
    find(SortedMap("Operation" -> "ItemLookup", "ItemId" -> isbn, "IdType" -> "ISBN"), DEFAULT_TIMEOUT, 0)
  }

}
