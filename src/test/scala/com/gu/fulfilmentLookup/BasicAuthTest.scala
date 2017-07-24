package com.gu.fulfilmentLookup

import com.gu.fulfilmentLookup._
import org.scalatest.FlatSpec

class BasicAuthTest extends FlatSpec {

  val fakeConfig = new Config {
    override val user = "testUser"
    override val password = "testPass"
    override val stage = "CODE"
  }

  "decodeBasicAuth" should "produce the expected user and password" in {
    val (user, pass) = BasicAuth.decodeBasicAuth("Basic dGVzdFVzZXI6dGVzdFBhc3M=")
    assert(fakeConfig.user == user && fakeConfig.password == pass)
  }

  "validAuth" should "return false for a missing auth header" in {
    assert(BasicAuth.validAuth(None, fakeConfig) == false)
  }

  "validAuth" should "return false for an invalid password" in {
    assert(BasicAuth.validAuth(Some("Basic dGVzdFVzZXI6d3JvbmdQYXNz"), fakeConfig) == false)
  }

  "validAuth" should "return false for an invalid username" in {
    assert(BasicAuth.validAuth(Some("Basic d3JvbmdVc2VyOnRlc3RQYXNz"), fakeConfig) == false)
  }

  "validAuth" should "return true when given the correct credentials" in {
    assert(BasicAuth.validAuth(Some("Basic dGVzdFVzZXI6dGVzdFBhc3M="), fakeConfig) == true)
  }

}
