package controllers

import play.api.mvc.Controller
import model.{AmazonBookDataRepositoryComponent, MongoDbBookRepositoryComponent}
import play.api.Play

class BookController extends Controller with securesocial.core.SecureSocial {

  def bookRepositoryComponent = BookController.bookRepositoryComponent

  def googleAnalyticsCode = BookController.googleAnalyticsCode

  def adminUserProvider = BookController.adminUserProvider

  def adminUserId = BookController.adminUserId

  def adminUserCheck = BookController.adminUserCheck

}

private object BookController {

  val bookRepositoryComponent = new MongoDbBookRepositoryComponent with AmazonBookDataRepositoryComponent {}

  val googleAnalyticsCode = Play.current.configuration.getString("google.analytics.code").get

  val adminUserProvider = Play.current.configuration.getString("admin.user.provider").get
  val adminUserId = Play.current.configuration.getString("admin.user.id").get

  val adminUserCheck = WithProviderAndUserId(adminUserProvider, adminUserId)

}
