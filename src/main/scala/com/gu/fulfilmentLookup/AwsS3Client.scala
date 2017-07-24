package com.gu.fulfilmentLookup

import java.io.InputStream
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GetObjectRequest
import purecsv.unsafe.CSVReader
import scala.io.Source
import scala.util.Try

trait CsvClient {
  def getDeliveryRows(s3Stream: InputStream): Try[List[DeliveryRow]]
  def getDeliveryRowsFromS3(bucketName: String, subFolder: String, fileName: String): Try[List[DeliveryRow]]
}

object AwsS3Client extends CsvClient with Logging {

  val s3Client = AmazonS3ClientBuilder.defaultClient()

  override def getDeliveryRows(s3Stream: InputStream): Try[List[DeliveryRow]] = {
    val csvAsString = Source.fromInputStream(s3Stream).mkString
    val parseAttempt = Try(CSVReader[DeliveryRow].readCSVFromString(csvAsString, skipHeader = true))
    s3Stream.close()
    parseAttempt
  }

  override def getDeliveryRowsFromS3(bucketName: String, subFolder: String, fileName: String): Try[List[DeliveryRow]] = {
    val key = s"${subFolder}${fileName}"
    val request = new GetObjectRequest(bucketName, key)
    val csvStream = Try(s3Client.getObject(request).getObjectContent)
    logger.info(s"Getting file from S3. Bucket: $bucketName | Key: $key")
    for {
      stream <- csvStream
      deliveryRows <- getDeliveryRows(stream)
    } yield deliveryRows
  }

}
