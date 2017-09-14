package com.gu.fulfilmentLookup

import java.lang.System.getenv

trait Config {
  def stage: String
  def salesforceUrl: String
  def salesforceClientId: String
  def salesforceClientSecret: String
  def salesforceUsername: String
  def salesforcePassword: String
  def salesforceToken: String
}

object EnvConfig extends Config {
  override def stage: String = getenv("Stage")
  override val salesforceUrl: String = getenv("sfUrl")
  override val salesforceClientId: String = getenv("sfClientId")
  override val salesforceClientSecret: String = getenv("sfClientSecret")
  override val salesforceUsername: String = getenv("sfUser")
  override val salesforcePassword: String = getenv("sfPass")
  override val salesforceToken: String = getenv("sfToken")
}