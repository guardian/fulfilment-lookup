package com.gu.fulfilmentLookup

import java.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json.{ JsPath, Json, Reads }

case class AuthHeader(authorisation: String)

case class LookupRequest(subscriptionName: String, date: LocalDate)

object LookupRequest {

  implicit val lookupRequestReads: Reads[LookupRequest] = (
    (JsPath \ "subscriptionName").read[String] and
    (JsPath \ "date").read[LocalDate]
  )(LookupRequest.apply _)
}

object AuthHeader {
  implicit val authHeaderReads = Json.reads[AuthHeader]
}

case class DeliveryRow(
  subscriptionName: String,
  contractId: String = "",
  fullName: String = "",
  jobTitle: String = "",
  company: String = "",
  department: String = "",
  addressLine1: String,
  addressLine2: String,
  addressLine3: String,
  town: String,
  postCode: String,
  quantity: String = "",
  telephone: String = "",
  propertyType: String = "",
  frontDoorAccess: String = "",
  doorColour: String = "",
  houseDetails: String = "",
  whereToLeave: String = "",
  landmarks: String = "",
  additionalInformation: String = "",
  letterBox: String = "",
  sourceCampaign: String = "",
  sentDate: String = "",
  deliveryDate: String = "",
  returnedDate: String = "",
  deliveryProblem: String = "",
  deliveryProblemNotes: String = "",
  chargeDay: String = ""
)
