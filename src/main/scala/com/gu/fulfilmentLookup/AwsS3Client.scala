package com.gu.fulfilmentLookup

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GetObjectRequest
import purecsv.unsafe.CSVReader
import scala.io.Source
import scala.util.Try

trait CsvClient {
  def getDeliveryRows(csvString: String): Try[List[DeliveryRow]]
  def getDeliveryRowsFromS3(bucketName: String, subFolder: String, fileName: String): Try[List[DeliveryRow]]
}

object AwsS3Client extends CsvClient with Logging {

  val s3Client = AmazonS3ClientBuilder.defaultClient()

  override def getDeliveryRows(csvString: String): Try[List[DeliveryRow]] = {
    Try(CSVReader[DeliveryRow].readCSVFromString(csvString, skipHeader = true))
  }

  override def getDeliveryRowsFromS3(bucketName: String, subFolder: String, fileName: String): Try[List[DeliveryRow]] = {
    val key = s"${subFolder}${fileName}"
    val request = new GetObjectRequest(bucketName, key)
    logger.info(s"Getting file from S3. Bucket: $bucketName | Key: $key")
    for {
      csvStream <- Try(s3Client.getObject(request).getObjectContent)
      csvString <- Try(Source.fromInputStream(csvStream).mkString)
      _ <- Try(csvStream.close())
      deliveryRows <- getDeliveryRows(csvString)
    } yield deliveryRows
  }

}
