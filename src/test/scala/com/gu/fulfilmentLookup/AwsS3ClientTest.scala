package com.gu.fulfilmentLookup

import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar

class AwsS3ClientTest extends FlatSpec with MockitoSugar with Logging {

  val parseFailStream = getClass.getResourceAsStream("/fulfilmentLookup/invalidDelivery.csv")
  val parseSuccessStream = getClass.getResourceAsStream("/fulfilmentLookup/validDelivery.csv")

  "getDeliveryRows" should "return a Failure if getDeliveryRows fails to parse the CSV" in {
    val tryRows = AwsS3Client.getDeliveryRows(parseFailStream)
    assert(tryRows.isFailure)
  }

  "getDeliveryRows" should "return a Success if getDeliveryRows parses the CSV" in {
    val tryRows = AwsS3Client.getDeliveryRows(parseSuccessStream)
    assert(tryRows.isSuccess)
  }

}
