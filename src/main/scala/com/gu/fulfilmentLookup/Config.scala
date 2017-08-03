package com.gu.fulfilmentLookup

import java.lang.System.getenv
import com.gu.fulfilmentLookup.BasicAuth.AuthDetails

trait Config {
  def authDetails: AuthDetails
  def stage: String
}

object EnvConfig extends Config {
  override def authDetails: AuthDetails = AuthDetails(getenv("User"), getenv("Password"))
  override def stage: String = getenv("Stage")
}