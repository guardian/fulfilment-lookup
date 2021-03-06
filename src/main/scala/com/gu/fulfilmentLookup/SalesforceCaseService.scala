package com.gu.fulfilmentLookup

import java.util.concurrent.TimeUnit
import okhttp3._
import play.api.libs.json.{ JsPath, JsSuccess, Json, Reads }
import play.api.libs.functional.syntax._
import scalaz.{ -\/, \/, \/- }

object SalesforceRequestWiring extends Logging {

  case class SalesforceAuth(accessToken: String, instanceUrl: String)

  object SalesforceAuth {

    implicit val salesforceAuthReads: Reads[SalesforceAuth] = (
      (JsPath \ "access_token").read[String] and
      (JsPath \ "instance_url").read[String]
    )(SalesforceAuth.apply _)

  }

  val restClient = new OkHttpClient().newBuilder()
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  def requestBuilder(config: Config, route: String): Request.Builder = {
    new Request.Builder()
      .url(s"${config.salesforceUrl}$route")
  }

  def withSfAuth(requestBuilder: Request.Builder, salesforceAuth: SalesforceAuth): Request.Builder = {
    requestBuilder.addHeader("Authorization", s"Bearer ${salesforceAuth.accessToken}")
  }

  def authenticate(config: Config): String \/ SalesforceAuth = {
    val builder = requestBuilder(config, "/services/oauth2/token")
    val formBody = new FormBody.Builder()
      .add("client_id", config.salesforceClientId)
      .add("client_secret", config.salesforceClientSecret)
      .add("username", config.salesforceUsername)
      .add("password", config.salesforcePassword + config.salesforceToken)
      .add("grant_type", "password")
      .build()
    val request = builder.post(formBody).build()
    logger.info(s"Attempting to perform Salesforce Authentication")
    val response = restClient.newCall(request).execute()
    val responseBody = Json.parse(response.body().string())
    responseBody.validate[SalesforceAuth] match {
      case JsSuccess(result, _) =>
        logger.info(s"Successful Salesforce authentication.")
        \/-(result)
      case _ =>
        -\/(s"Failed to authenticate with Salesforce | body was: ${responseBody.toString}")
    }
  }

}

case class LookupResult(fileChecked: String, subscriptionInFile: Boolean, addressDetails: Option[String])

trait RaiseCase {
  def open(config: Config, lookupRequest: LookupRequest, lookupResult: LookupResult): String \/ Boolean
}

object RaiseSalesforceCase extends RaiseCase with Logging {

  import com.gu.fulfilmentLookup.SalesforceRequestWiring._

  def description(lookupResult: LookupResult): String = {

    val addressInformation = lookupResult.addressDetails.map {
      address => s"We asked our fulfilment partner to send the paper to: $address"
    }.getOrElse("")

    "This case has been automatically raised due to a customer-initiated distribution check \n" +
      "\n" +
      s"Fulfilment File checked: ${lookupResult.fileChecked} \n" +
      s"Subscription Number included in file: ${lookupResult.subscriptionInFile} \n" +
      addressInformation

  }

  override def open(config: Config, lookupRequest: LookupRequest, lookupResult: LookupResult): String \/ Boolean = {
    val salesforceAuth = authenticate(config)
    salesforceAuth.flatMap { auth =>
      val builderWithAuth = withSfAuth(requestBuilder(config, "/services/data/v29.0/sobjects/Case/"), auth)
      val debugInfo = description(lookupResult)
      val bodyString = Json.obj(
        "ContactId" -> lookupRequest.sfContactId,
        "Status" -> "New",
        "Origin" -> "FulfilmentLookupAPI",
        "Product__c" -> "Home Delivery",
        "Journey__c" -> "CS - Home Delivery Support", // Case Type
        "Case_Closure_Reason__c" -> "No Delivery", // Sub-Category
        "Subject" -> s"Delivery Problem | Paper Date: ${lookupRequest.date}",
        "Description" -> s"$debugInfo"
      ).toString()
      val request = builderWithAuth.post(RequestBody.create(MediaType.parse("application/json"), bodyString)).build()
      logger.info(s"Attempting to raise case in Salesforce for sfContactId: ${lookupRequest.sfContactId}")
      val response = restClient.newCall(request).execute()
      if (response.isSuccessful) { \/-(true) }
      else {
        logger.error(s"Salesforce call failed with a ${response.code()} | body: ${response.body.string}")
        -\/(s"Failed to raise Salesforce case for sfContactId: ${lookupRequest.sfContactId}")
      }
    }
  }

}
