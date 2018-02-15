package com.gu.fulfilmentLookup

import java.io.{ InputStream, OutputStream, OutputStreamWriter }
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import play.api.libs.json.{ JsError, JsSuccess, JsValue, Json }
import com.amazonaws.services.lambda.runtime.Context
import com.gu.fulfilmentLookup.ResponseWriters._
import scala.util.{ Failure, Success, Try }
import scalaz.{ -\/, \/- }

trait FulfilmentLookupLambda extends Logging {

  def csvClient: CsvClient
  def raiseCase: RaiseCase
  def stage: String
  def loadConfig: Try[Config]

  // Entry point for the Lambda
  def handler(inputStream: InputStream, outputStream: OutputStream, context: Context): Unit = {
    logger.info(s"Fulfilment Lookup Lambda is starting up in $stage")
    val inputEvent = Json.parse(inputStream)
    val maybeBody = (inputEvent \ "body").toOption
    maybeBody match {
      case Some(body) => {
        Json.fromJson[LookupRequest](Json.parse(body.as[String])) match {
          case validLookup: JsSuccess[LookupRequest] => {
            logger.info(s"Received request the following data: ${validLookup.value}")
            loadConfig match {
              case Success(config) => {
                val response = lookUp(config, validLookup.value, outputStream)
                outputForAPIGateway(outputStream, Json.toJson(response))
              }
              case Failure(error) => {
                logger.error(s"Failed to load config in $stage due to $error")
                LookupResponse(500, s"Failed to load config in $stage")
              }
            }

          }
          case error: JsError => {
            logger.error(s"Failed to parse body successfully, we got \n ${body.as[String]}")
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
  }

  // Main logic happens here
  def lookUp(config: Config, lookupRequest: LookupRequest, outputStream: OutputStream): LookupResponse = {
    val bucket = s"fulfilment-export-${stage.toLowerCase}"
    val subFolder = "uploaded/"
    val fileName = sfFilename(lookupRequest.date)
    val deliveryRows = csvClient.getDeliveryRowsFromS3(bucket, subFolder, fileName)
    deliveryRows match {
      case Success(rows) => {
        logger.info(s"Successfully retrieved fulfilment file and parsed ${rows.size} row(s) from $fileName")
        val subNames = rows.map(row => row.subscriptionName)
        val subInFile = subNames.contains(lookupRequest.subscriptionName)
        val subIndex = subNames.indexOf(lookupRequest.subscriptionName)
        val result = LookupResult(
          fileName,
          subInFile,
          populateAddressRecord(rows, subIndex)
        )
        logger.info(s"Performed successful lookup for ${lookupRequest.subscriptionName} in $fileName. subInFile: $subInFile | subIndex: $subIndex")
        raiseCase.open(config, lookupRequest, result) match {
          case \/-(_) =>
            logger.info("Successfully raised Salesforce case")
            LookupResponse(200, responseBodyAsString(LookupResponseBody(result.fileChecked, result.subscriptionInFile)))
          case -\/(error) =>
            logger.error(error)
            LookupResponse(500, error)
        }
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
    val dateFormatter = DateTimeFormatter.ofPattern("dd_MM_yyyy")
    val sfFileFormattedDate = date.format(dateFormatter)
    s"HOME_DELIVERY_${dayOfWeek}_${sfFileFormattedDate}.csv"
  }

  //This is slightly horrible, but is required as it seems the response body
  //for a Proxy Integration must be a String (and not a JSON object)
  def responseBodyAsString(lookupResponseBody: LookupResponseBody) = {
    Json.stringify(Json.toJson(lookupResponseBody))
  }

  def outputForAPIGateway(outputStream: OutputStream, js: JsValue): Unit = {
    val writer = new OutputStreamWriter(outputStream, "UTF-8")
    val jsonString = Json.stringify(js)
    writer.write(jsonString)
    writer.close()
  }

}

object Lambda extends FulfilmentLookupLambda {
  override val csvClient = FulfilmentFileClient
  override val raiseCase = RaiseSalesforceCase
  override val stage = System.getenv("Stage")
  override val loadConfig = Config.load(stage)
}

