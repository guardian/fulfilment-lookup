package com.gu.fulfilmentLookup

import com.gu.fulfilmentLookup.BasicAuth.AuthDetails
import org.scalatest.FlatSpec

class BasicAuthTest extends FlatSpec {

  val fakeConfig = new Config {
    override val authDetails = AuthDetails("testUser", "testPass")
    override val stage = "CODE"
  }

  "decodeBasicAuth" should "produce the expected user and password" in {
    val maybeAuth = BasicAuth.decodeBasicAuth("Basic dGVzdFVzZXI6dGVzdFBhc3M=")
    assert(fakeConfig.authDetails == maybeAuth.get)
  }

  "validAuth" should "return false for a missing auth header" in {
    assert(BasicAuth.validAuth(None, fakeConfig.authDetails) == false)
  }

  "validAuth" should "return false for an invalid password" in {
    assert(BasicAuth.validAuth(Some("Basic dGVzdFVzZXI6d3JvbmdQYXNz"), fakeConfig.authDetails) == false)
  }

  "validAuth" should "return false for an invalid username" in {
    assert(BasicAuth.validAuth(Some("Basic d3JvbmdVc2VyOnRlc3RQYXNz"), fakeConfig.authDetails) == false)
  }

  "validAuth" should "return true when given the correct credentials" in {
    assert(BasicAuth.validAuth(Some("Basic dGVzdFVzZXI6dGVzdFBhc3M="), fakeConfig.authDetails) == true)
  }

}
