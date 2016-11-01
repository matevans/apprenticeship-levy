/*
 * Copyright 2016 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apprenticeshiplevy.controllers

import play.api.hal.{HalLink, HalResource}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.api.controllers.{ErrorResponse, HeaderValidator}
import uk.gov.hmrc.play.microservice.controller.BaseController
import play.api.mvc.{ActionBuilder, Request, Result, Results, RequestHeader}

trait ApiController extends BaseController with HeaderValidator {

  implicit class ErrorResponseSyntax(er: ErrorResponse) {
    def result: Result = Status(er.httpStatusCode)(Json.toJson(er))
  }

  override implicit def hc(implicit rh: RequestHeader) = super.hc(rh).withExtraHeaders(rh.headers
                                                                                          .toSimpleMap
                                                                                          .filterKeys(_ == "Environment")
                                                                                          .headOption
                                                                                          .getOrElse(("Environment","clone")))

  val withValidAcceptHeader: ActionBuilder[Request] = validateAccept(acceptHeaderValidationRules)

  def selfLink(url: String): HalLink = HalLink("self", url)

  def ok(hal: HalResource): Result = Ok(Json.toJson(hal)).withHeaders("Content-Type" -> "application/hal+json")
}
