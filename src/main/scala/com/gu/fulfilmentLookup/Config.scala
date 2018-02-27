package com.gu.fulfilmentLookup

import com.amazonaws.services.s3.model.GetObjectRequest
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.util.{ Failure, Success, Try }

case class Config(
  salesforceUrl: String,
  salesforceClientId: String,
  salesforceClientSecret: String,
  salesforceUsername: String,
  salesforcePassword: String,
  salesforceToken: String
)

object Config extends Logging {

  implicit val configReads: Reads[Config] = (
    (JsPath \ "sfUrl").read[String] and
    (JsPath \ "sfClientId").read[String] and
    (JsPath \ "sfClientSecret").read[String] and
    (JsPath \ "sfUser").read[String] and
    (JsPath \ "sfPass").read[String] and
    (JsPath \ "sfToken").read[String]
  )(Config.apply _)

  def load(stage: String): Try[Config] = {
    logger.info(s"Attempting to load config in $stage")
    val bucket = s"gu-reader-revenue-private/membership/fulfilment-lookup/$stage"
    val key = "fulfilment-lookup.private.json"
    val request = new GetObjectRequest(bucket, key)
    for {
      string <- AwsS3.fetchString(request)
      config <- parseConfig(string)
    } yield config
  }

  def parseConfig(jsonConfig: String): Try[Config] = {
    Json.fromJson[Config](Json.parse(jsonConfig)) match {
      case validConfig: JsSuccess[Config] => Success(validConfig.value)
      case error: JsError => Failure(new ConfigFailure(error))
    }
  }

  class ConfigFailure(error: JsError) extends Throwable

}