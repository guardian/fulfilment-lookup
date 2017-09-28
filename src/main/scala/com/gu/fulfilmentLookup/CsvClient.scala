package com.gu.fulfilmentLookup

import com.amazonaws.services.s3.model.GetObjectRequest
import purecsv.unsafe.CSVReader
import scala.util.Try

trait CsvClient {
  def getDeliveryRows(csvString: String): Try[List[DeliveryRow]]
  def getDeliveryRowsFromS3(bucketName: String, subFolder: String, fileName: String): Try[List[DeliveryRow]]
}

object FulfilmentFileClient extends CsvClient {

  override def getDeliveryRows(csvString: String): Try[List[DeliveryRow]] = {
    Try(CSVReader[DeliveryRow].readCSVFromString(csvString, skipHeader = true))
  }

  override def getDeliveryRowsFromS3(bucketName: String, subFolder: String, fileName: String): Try[List[DeliveryRow]] = {
    val key = s"${subFolder}${fileName}"
    val request = new GetObjectRequest(bucketName, key)
    for {
      rows <- AwsS3.fetchString(request)
      deliveryRows <- getDeliveryRows(rows)
    } yield deliveryRows
  }

}
