package com.gu.fulfilmentLookup

import scala.util.{ Failure, Success, Try }

object BasicAuth extends Logging {

  case class AuthDetails(user: String, pass: String)

  def validAuth(suppliedAuth: Option[String], configAuthDetails: AuthDetails): Boolean = {
    val auth = suppliedAuth.flatMap(authString => decodeBasicAuth(authString).map(auth => auth == configAuthDetails))
    auth.getOrElse(false)
  }

  def decodeBasicAuth(authHeader: String): Option[AuthDetails] = {
    val encodedAuth = authHeader.replaceFirst("Basic ", "")
    val decodeAttempt = Try(new sun.misc.BASE64Decoder().decodeBuffer(encodedAuth))
    decodeAttempt match {
      case Success(decoded) => {
        val Array(user, pass) = new String(decoded, "UTF-8").split(":")
        Some(AuthDetails(user, pass))
      }
      case Failure(error) => {
        logger.info(s"Failed to decode auth due to $error")
        None
      }
    }
  }

}
