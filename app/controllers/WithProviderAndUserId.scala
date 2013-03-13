package controllers

import securesocial.core.{Identity, Authorization}

case class WithProviderAndUserId(providerId: String, userId: String) extends Authorization {
  def isAuthorized(user: Identity) = {
    user.id.providerId == providerId &&
      user.id.id == userId
  }
}
