package com.gu.fulfilmentLookup

import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import scala.io.Source

class AwsS3ClientTest extends FlatSpec with MockitoSugar with Logging {

  val parseFailString = Source.fromInputStream(getClass.getResourceAsStream("/fulfilmentLookup/invalidDelivery.csv")).mkString
  val parseSuccessString = Source.fromInputStream(getClass.getResourceAsStream("/fulfilmentLookup/validDelivery.csv")).mkString

  "getDeliveryRows" should "return a Failure if getDeliveryRows fails to parse the CSV" in {
    val tryRows = AwsS3Client.getDeliveryRows(parseFailString)
    assert(tryRows.isFailure)
  }

  "getDeliveryRows" should "return a Success if getDeliveryRows parses the CSV" in {
    val tryRows = AwsS3Client.getDeliveryRows(parseSuccessString)
    assert(tryRows.isSuccess)
  }

}
