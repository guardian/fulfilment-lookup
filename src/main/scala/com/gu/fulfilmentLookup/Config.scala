package com.gu.fulfilmentLookup

import java.lang.System.getenv

trait Config {
  def user: String
  def password: String
  def stage: String
}

object EnvConfig extends Config {
  override def user: String = getenv("User")
  override def password: String = getenv("Password")
  override def stage: String = getenv("Stage")
}