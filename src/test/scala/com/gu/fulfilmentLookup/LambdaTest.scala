package com.gu.fulfilmentLookup

import java.io.ByteArrayOutputStream
import java.time.LocalDate
import com.amazonaws.AmazonServiceException
import com.gu.fulfilmentLookup.SalesforceRequestWiring.SalesforceAuth
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.Matchers._
import scala.util.{ Failure, Success }
import scalaz.{ -\/, \/- }

class LambdaTest extends FlatSpec with MockitoSugar {

  val fakeFulfilmentClient = mock[CsvClient]

  val fakeSfCaseRaiser = mock[RaiseCase]

  val fakeSfAuth = SalesforceAuth("token123", "fakeUrl")

  val fakeConfig = Config(
    salesforceUrl = "sfUrl",
    salesforceClientId = "sfClientId",
    salesforceClientSecret = "sfClientSecret",
    salesforceUsername = "sfUser",
    salesforcePassword = "sfPass",
    salesforceToken = "sfToken"
  )

  val successfulConfigLoad = Success(fakeConfig)

  val lambda = new FulfilmentLookupLambda {
    override def csvClient: CsvClient = fakeFulfilmentClient
    override def raiseCase: RaiseCase = fakeSfCaseRaiser
    override def stage: String = "DEV"
    override def loadConfig = successfulConfigLoad
  }

  val fakeDeliveryRowA = DeliveryRow(
    subscriptionName = "A-S12345",
    addressLine1 = "House 123",
    addressLine2 = "The Street",
    addressLine3 = "Islington",
    town = "London",
    postCode = "N1 9AG"
  )

  val fakeDeliveryRowB = DeliveryRow(
    subscriptionName = "A-S54321",
    addressLine1 = "House 123",
    addressLine2 = "The Street",
    addressLine3 = "Islington",
    town = "London",
    postCode = "N1 9AG"
  )

  val date = LocalDate.of(2017, 7, 21)

  val expectedAddress = "House 123, The Street, Islington, London, N1 9AG"
  val deliveryRows = List(fakeDeliveryRowB, fakeDeliveryRowA)

  val lookupRequestA = LookupRequest("A-S12345", "sf12345", date)
  val lookupRequestB = LookupRequest("A-S67815", "sf67891", date)

  val presentLookupResponse = LookupResponseBody("HOME_DELIVERY_Friday_21_07_2017.csv", true)
  val missingLookupResponse = LookupResponseBody("HOME_DELIVERY_Friday_21_07_2017.csv", false)

  val presentLookupResult = LookupResult("HOME_DELIVERY_Friday_21_07_2017.csv", true, Some(expectedAddress))
  val missingLookupResult = LookupResult("HOME_DELIVERY_Friday_21_07_2017.csv", false, None)

  // Tests for individual methods
  "sfFilename" should "build a valid Salesforce Fulfilment File name" in {
    assert(lambda.sfFilename(date) == "HOME_DELIVERY_Friday_21_07_2017.csv")
  }

  "fullAddress" should "construct a valid address" in {
    assert(lambda.fullAddress(fakeDeliveryRowA) == expectedAddress)
  }

  "populateAddressRecord" should "build an address record correctly if the sub is present in the file" in {
    assert(lambda.populateAddressRecord(deliveryRows, 1) == Some(expectedAddress))
  }

  "populateAddressRecord" should "return a none if the sub is not in the file" in {
    assert(lambda.populateAddressRecord(deliveryRows, -1) == None)
  }

  "lookUp" should "build a correct LookupResponse when a subscription is present" in {
    when(fakeFulfilmentClient.getDeliveryRowsFromS3("fulfilment-export-dev", "uploaded/", "HOME_DELIVERY_Friday_21_07_2017.csv")).thenReturn(Success(deliveryRows))
    when(fakeSfCaseRaiser.open(fakeConfig, lookupRequestA, presentLookupResult)).thenReturn(\/-(true))
    assert(lambda.lookUp(fakeConfig, lookupRequestA, new ByteArrayOutputStream) == LookupResponse(200, lambda.responseBodyAsString(presentLookupResponse)))
  }

  "lookUp" should "build a correct LookupResponse when a subscription is missing" in {
    when(fakeFulfilmentClient.getDeliveryRowsFromS3("fulfilment-export-dev", "uploaded/", "HOME_DELIVERY_Friday_21_07_2017.csv")).thenReturn(Success(deliveryRows))
    when(fakeSfCaseRaiser.open(fakeConfig, lookupRequestB, missingLookupResult)).thenReturn(\/-(true))
    assert(lambda.lookUp(fakeConfig, lookupRequestB, new ByteArrayOutputStream) == LookupResponse(200, lambda.responseBodyAsString(missingLookupResponse)))
  }

  "lookUp" should "return an error when there is a problem getting delivery rows from S3" in {
    when(fakeFulfilmentClient.getDeliveryRowsFromS3("fulfilment-export-dev", "uploaded/", "HOME_DELIVERY_Friday_21_07_2017.csv")).thenReturn(Failure(new AmazonServiceException("Error from S3")))
    assert(lambda.lookUp(fakeConfig, lookupRequestB, new ByteArrayOutputStream) == LookupResponse(500, "Failed to retrieve fulfilment records"))
  }

  "lookUp" should "return an error if we fail to raise a case in Salesforce" in {
    when(fakeFulfilmentClient.getDeliveryRowsFromS3("fulfilment-export-dev", "uploaded/", "HOME_DELIVERY_Friday_21_07_2017.csv")).thenReturn(Success(deliveryRows))
    when(fakeSfCaseRaiser.open(fakeConfig, lookupRequestA, presentLookupResult)).thenReturn(-\/("Failed to raise SF case"))
    assert(lambda.lookUp(fakeConfig, lookupRequestA, new ByteArrayOutputStream) == LookupResponse(500, "Failed to raise SF case"))
  }

  //Tests for complete Lambda processing
  def responseString(outputStream: ByteArrayOutputStream) = new String(outputStream.toByteArray(), "UTF-8")
  implicit val lookupResponseBodyWriter = ResponseWriters.lookupResponseBodyWrites

  "handler" should "perform a successful lookup when a valid request is made and the sub name is found" in {
    val inputStream = getClass.getResourceAsStream("/fulfilmentLookup/validRequestSubInFile.json")
    val outputStream = new ByteArrayOutputStream
    when(fakeFulfilmentClient.getDeliveryRowsFromS3("fulfilment-export-dev", "salesforce_output/", "HOME_DELIVERY_Friday_21_07_2017.csv")).thenReturn(Success(deliveryRows))
    when(fakeSfCaseRaiser.open(fakeConfig, lookupRequestA, presentLookupResult)).thenReturn(\/-(true))
    lambda.handler(inputStream, outputStream, null)
    val responseString = new String(outputStream.toByteArray(), "UTF-8")
    val expected =
      s"""{"statusCode":200,"headers":{"Content-Type":"application/json"},"body":"{\\"fileChecked\\":\\"HOME_DELIVERY_Friday_21_07_2017.csv\\",\\"subscriptionInFile\\":true}"}"""
    assert(responseString == expected)
  }

  "handler" should "perform a successful lookup when a valid request is made and the sub name is NOT found" in {
    val inputStream = getClass.getResourceAsStream("/fulfilmentLookup/validRequestSubNotInFile.json")
    val outputStream = new ByteArrayOutputStream
    when(fakeFulfilmentClient.getDeliveryRowsFromS3("fulfilment-export-dev", "salesforce_output/", "HOME_DELIVERY_Friday_21_07_2017.csv")).thenReturn(Success(deliveryRows))
    when(fakeSfCaseRaiser.open(fakeConfig, lookupRequestB, missingLookupResult)).thenReturn(\/-(true))
    lambda.handler(inputStream, outputStream, null)
    val responseString = new String(outputStream.toByteArray(), "UTF-8")
    val expected =
      s"""{"statusCode":200,"headers":{"Content-Type":"application/json"},"body":"{\\"fileChecked\\":\\"HOME_DELIVERY_Friday_21_07_2017.csv\\",\\"subscriptionInFile\\":false}"}"""
    assert(responseString == expected)
  }

  "handler" should "return a 400 if there is no body in the request" in {
    val inputStream = getClass.getResourceAsStream("/fulfilmentLookup/invalidRequestNoBody.json")
    val outputStream = new ByteArrayOutputStream
    lambda.handler(inputStream, outputStream, null)
    val responseString = new String(outputStream.toByteArray(), "UTF-8")
    val expected = s"""{"statusCode":400,"headers":{"Content-Type":"application/json"},"body":"No request body found in input event"}"""
    assert(responseString == expected)
  }

  "handler" should "return a 400 if there is a body but it cannot be parsed successfully" in {
    val inputStream = getClass.getResourceAsStream("/fulfilmentLookup/invalidRequestInvalidBody.json")
    val outputStream = new ByteArrayOutputStream
    lambda.handler(inputStream, outputStream, null)
    val responseString = new String(outputStream.toByteArray(), "UTF-8")
    val expected = s"""{"statusCode":400,"headers":{"Content-Type":"application/json"},"body":"Failed to parse body successfully"}"""
    assert(responseString == expected)
  }

}
