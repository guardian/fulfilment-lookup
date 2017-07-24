package com.gu.fulfilmentLookup

import java.time.LocalDate

import play.api.libs.json.{ JsValue, Json, Writes }

case class LookupResponseBody(subscriptionName: String, date: LocalDate, fileChecked: String, subscriptionInFile: Boolean, addressDetails: Option[String])

case class LookupResponse(statusCode: Int = 200, body: String)

object ResponseWriters {

  implicit val lookupResponseBodyWrites = new Writes[LookupResponseBody] {
    def writes(lookupResponseBody: LookupResponseBody): JsValue = Json.obj(
      "subscriptionName" -> lookupResponseBody.subscriptionName,
      "date" -> lookupResponseBody.date,
      "fileChecked" -> lookupResponseBody.fileChecked,
      "subscriptionInFile" -> lookupResponseBody.subscriptionInFile,
      "addressDetails" -> lookupResponseBody.addressDetails
    )
  }

  implicit val lookupResponseWrites = new Writes[LookupResponse] {
    def writes(response: LookupResponse): JsValue = Json.obj(
      "statusCode" -> response.statusCode,
      "headers" -> Json.obj("Content-Type" -> "application/json"),
      "body" -> response.body
    )
  }

}

