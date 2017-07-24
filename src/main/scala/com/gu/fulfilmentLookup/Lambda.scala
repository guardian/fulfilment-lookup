package com.gu.fulfilmentLookup

import java.io.{ InputStream, OutputStream, OutputStreamWriter }
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import play.api.libs.json.{ JsError, JsSuccess, JsValue, Json }
import com.amazonaws.services.lambda.runtime.Context
import com.gu.fulfilmentLookup.ResponseWriters._
import scala.util.{ Failure, Success }

trait FulfilmentLookupLambda extends Logging {

  def s3Client: CsvClient
  def config: Config

  // Entry point for the Lambda
  def handler(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit = {
    logger.info(s"Fulfilment Lookup Lambda is starting up...")
    val inputEvent = Json.parse(inputStream)
    val maybeAuthHeader = (inputEvent \ "headers" \ "Authorization").asOpt[String]
    if (BasicAuth.validAuth(maybeAuthHeader, config)) {
      val maybeBody = (inputEvent \ "body").toOption
      maybeBody match {
        case Some(body) => {
          Json.fromJson[LookupRequest](Json.parse(body.as[String])) match {
            case validLookup: JsSuccess[LookupRequest] => {
              logger.info(s"Received request the following data: ${validLookup.value}")
              val response = lookUp(validLookup.value, outputStream)
              outputForAPIGateway(outputStream, Json.toJson(response))
            }
            case error: JsError => {
              val message = "Failed to parse body successfully"
              logger.error(message)
              val response = LookupResponse(400, "Failed to parse body successfully")
              outputForAPIGateway(outputStream, Json.toJson(response))
            }
          }
        }
        case None => {
          val message = "No request body found in input event"
          logger.error(message)
          val response = LookupResponse(400, message)
          outputForAPIGateway(outputStream, Json.toJson(response))
        }
      }
    } else {
      val message = "Credentials are missing or invalid"
      logger.info(message)
      val response = LookupResponse(401, message)
      outputForAPIGateway(outputStream, Json.toJson(response))
    }

  }

  // Main logic happens here
  def lookUp(lookupRequest: LookupRequest, outputStream: OutputStream): LookupResponse = {
    val stage = config.stage
    val bucket = "fulfilment-output-test"
    val subFolder = s"${stage}/salesforce_output/"
    val fileName = sfFilename(lookupRequest.date)
    val deliveryRows = s3Client.getDeliveryRowsFromS3(bucket, subFolder, fileName)
    deliveryRows match {
      case Success(rows) => {
        logger.info(s"Successfully retrieved fulfilment file and parsed ${rows.size} row(s) from $fileName")
        val subNames = rows.map(row => row.subscriptionName)
        val subInFile = subNames.contains(lookupRequest.subscriptionName)
        val subIndex = subNames.indexOf(lookupRequest.subscriptionName)
        val responseBody = LookupResponseBody(
          lookupRequest.subscriptionName,
          lookupRequest.date,
          fileName,
          subInFile,
          populateAddressRecord(rows, subIndex)
        )
        LookupResponse(200, responseBodyAsString(responseBody))
      }
      case Failure(error) => {
        logger.error(s"Failed to get or parse delivery rows due to: $error. S3 bucket: $bucket | subFolder: $subFolder | fileName: $fileName")
        LookupResponse(500, "Failed to retrieve fulfilment records")
      }
    }
  }

  def populateAddressRecord(rows: List[DeliveryRow], subIndex: Int): Option[String] = {
    if (subIndex >= 0) {
      Some(fullAddress(rows(subIndex)))
    } else None
  }

  def fullAddress(row: DeliveryRow): String = {
    s"${row.addressLine1}, ${row.addressLine2}, ${row.addressLine3}, ${row.town}, ${row.postCode}"
  }

  def sfFilename(date: LocalDate): String = {
    val dayOfWeek = date.getDayOfWeek.toString.toLowerCase.capitalize
    val dateFormatter = DateTimeFormatter.ofPattern("dd_MM_YYYY")
    val sfFileFormattedDate = date.format(dateFormatter)
    s"HOME_DELIVERY_${dayOfWeek}${sfFileFormattedDate}.csv"
  }

  //This is slightly horrible, but is required as it seems the response body
  //for a Proxy Integration must be a String (and not a JSON object)
  def responseBodyAsString(lookupResponseBody: LookupResponseBody) = {
    Json.stringify(Json.toJson(lookupResponseBody))
  }

  def outputForAPIGateway(outputStream: OutputStream, js: JsValue): Unit = {
    val writer = new OutputStreamWriter(outputStream, "UTF-8")
    val jsonString = Json.stringify(js)
    logger.info(s"Response will be: $jsonString}")
    writer.write(jsonString)
    writer.close()
  }

}

object Lambda extends FulfilmentLookupLambda {
  override val s3Client = AwsS3Client
  override val config = EnvConfig
}

