package views.securesocial

import securesocial.controllers.{DefaultTemplatesPlugin, TemplatesPlugin}
import play.api.mvc.{RequestHeader, Request}
import play.api.data.Form
import play.api.templates.{Txt, Html}
import securesocial.controllers.Registration.RegistrationInfo
import play.api.Play
import securesocial.core.{Identity, SecuredRequest}
import securesocial.controllers.PasswordChange.ChangeInfo

class MyViews(application: play.api.Application) extends TemplatesPlugin {

  private val defaultImpl = new DefaultTemplatesPlugin(application)
  private val googleAnalyticsCode = Play.current.configuration.getString("google.analytics.code").get

  /**
   * Returns the html for the login page
   * @param request
   * @tparam A
   * @return
   */
  override def getLoginPage[A](implicit request: Request[A], form: Form[(String, String)],
                               msg: Option[String] = None): Html = {
    implicit val user: Option[securesocial.core.Identity] = None
    views.html.login(form, msg, googleAnalyticsCode)
  }

  /**
   * Returns the html for the signup page
   *
   * @param request
   * @tparam A
   * @return
   */
  override def getSignUpPage[A](implicit request: Request[A], form: Form[RegistrationInfo], token: String): Html = {
    defaultImpl.getSignUpPage
  }

  /**
   * Returns the html for the start signup page
   *
   * @param request
   * @tparam A
   * @return
   */
  override def getStartSignUpPage[A](implicit request: Request[A], form: Form[String]): Html = {
    defaultImpl.getStartSignUpPage
  }

  /**
   * Returns the html for the reset password page
   *
   * @param request
   * @tparam A
   * @return
   */
  override def getStartResetPasswordPage[A](implicit request: Request[A], form: Form[String]): Html = {
    defaultImpl.getStartResetPasswordPage
  }

  /**
   * Returns the html for the start reset page
   *
   * @param request
   * @tparam A
   * @return
   */
  override def getResetPasswordPage[A](implicit request: Request[A], form: Form[(String, String)], token: String): Html = {
    defaultImpl.getResetPasswordPage
  }

  /**
   * Returns the html for the change password page
   *
   * @param request
   * @param form
   * @tparam A
   * @return
   */
  override def getPasswordChangePage[A](implicit request: SecuredRequest[A], form: Form[ChangeInfo]): Html = {
    defaultImpl.getPasswordChangePage
  }


  /**
   * Returns the email sent when a user starts the sign up process
   *
   * @param token the token used to identify the request
   * @param request the current http request
   * @return a String with the html code for the email
   */
  override def getSignUpEmail(token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    defaultImpl.getSignUpEmail(token)
  }

  /**
   * Returns the email sent when the user is already registered
   *
   * @param user the user
   * @param request the current request
   * @return a String with the html code for the email
   */
  override def getAlreadyRegisteredEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    defaultImpl.getAlreadyRegisteredEmail(user)
  }

  /**
   * Returns the welcome email sent when the user finished the sign up process
   *
   * @param user the user
   * @param request the current request
   * @return a String with the html code for the email
   */
  override def getWelcomeEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    defaultImpl.getWelcomeEmail(user)
  }

  /**
   * Returns the email sent when a user tries to reset the password but there is no account for
   * that email address in the system
   *
   * @param request the current request
   * @return a String with the html code for the email
   */
  override def getUnknownEmailNotice()(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    defaultImpl.getUnknownEmailNotice()
  }

  /**
   * Returns the email sent to the user to reset the password
   *
   * @param user the user
   * @param token the token used to identify the request
   * @param request the current http request
   * @return a String with the html code for the email
   */
  override def getSendPasswordResetEmail(user: Identity, token: String)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    defaultImpl.getSendPasswordResetEmail(user, token)
  }

  /**
   * Returns the email sent as a confirmation of a password change
   *
   * @param user the user
   * @param request the current http request
   * @return a String with the html code for the email
   */
  override def getPasswordChangedNoticeEmail(user: Identity)(implicit request: RequestHeader): (Option[Txt], Option[Html]) = {
    defaultImpl.getPasswordChangedNoticeEmail(user)
  }

  override def getNotAuthorizedPage[A](implicit request: Request[A]): Html = {
    defaultImpl.getNotAuthorizedPage
  }

}