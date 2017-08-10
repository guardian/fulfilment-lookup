package com.gu.fulfilmentLookup

import java.lang.System.getenv

trait Config {
  def stage: String
}

object EnvConfig extends Config {
  override def stage: String = getenv("Stage")
}