package com.gu.fulfilmentLookup

object BasicAuth extends Logging {

  def validAuth(auth: Option[String], config: Config): Boolean = {
    auth match {
      case Some(auth) => {
        val userDetails = decodeBasicAuth(auth)
        val user = userDetails._1
        val pass = userDetails._2
        user == config.user && pass == config.password
      }
      case None => {
        false
      }
    }
  }

  def decodeBasicAuth(authHeader: String): (String, String) = {
    val encodedAuth = authHeader.replaceFirst("Basic ", "")
    val decoded = new sun.misc.BASE64Decoder().decodeBuffer(encodedAuth)
    val Array(user, password) = {
      val decodedString = new String(decoded)
      decodedString.split(":")
    }
    (user, password)
  }

}
