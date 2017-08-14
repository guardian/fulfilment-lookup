package com.gu.fulfilmentLookup

import java.util.concurrent.TimeUnit
import okhttp3._
import play.api.libs.json.{ JsPath, JsSuccess, Json, Reads }
import play.api.libs.functional.syntax._
import scalaz.{ -\/, \/, \/- }

case class SalesforceAuth(accessToken: String, instanceUrl: String)

object SalesforceAuth {

  implicit val salesforceAuthReads: Reads[SalesforceAuth] = (
    (JsPath \ "access_token").read[String] and
    (JsPath \ "instance_url").read[String]
  )(SalesforceAuth.apply _)

}

trait CaseService {
  def authenticate(config: Config): String \/ SalesforceAuth
  def raiseCase(config: Config, lookupRequest: LookupRequest, lookupResponseBody: LookupResponseBody): String \/ Boolean
}

object SalesforceCaseService extends CaseService with Logging {

  val restClient = new OkHttpClient().newBuilder()
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  def requestBuilder(config: Config, route: String): Request.Builder = {
    new Request.Builder()
      .url(s"${config.salesforceUrl}/$route")
  }

  def withSfAuth(requestBuilder: Request.Builder, salesforceAuth: SalesforceAuth): Request.Builder = {
    requestBuilder.addHeader("Authorization", s"Bearer ${salesforceAuth.accessToken}")
  }

  override def authenticate(config: Config): String \/ SalesforceAuth = {
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

  override def raiseCase(config: Config, lookupRequest: LookupRequest, lookupResponseBody: LookupResponseBody): String \/ Boolean = {
    val salesforceAuth = authenticate(config)
    salesforceAuth.flatMap { auth =>
      val builderWithAuth = withSfAuth(requestBuilder(config, "sobjects/Case/"), auth)
      val bodyString = Json.obj(
        "ContactId" -> lookupRequest.sfContactId,
        "Reason" -> "Non-delivery",
        "Status" -> "New",
        "Origin" -> "subscriptions",
        "Subject" -> s"Non-delivery | Paper Date: ${lookupRequest.date}",
        "Description" -> s"=== Debug Information === \n ${lookupResponseBody}"
      ).toString()
      val request = builderWithAuth.post(RequestBody.create(MediaType.parse("application/json"), bodyString)).build()
      logger.info(s"Attempting to raise case in Salesforce for sfContactId: ${lookupRequest.sfContactId}")
      val response = restClient.newCall(request).execute()
      if (response.isSuccessful) \/-(true) else -\/(s"Failed to raise Salesforce case for sfContactId: ${lookupRequest.sfContactId}")
    }
  }

}
