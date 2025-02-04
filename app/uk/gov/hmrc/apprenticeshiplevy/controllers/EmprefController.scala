/*
 * Copyright 2019 HM Revenue & Customs
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


import play.api.hal.{Hal, HalLink, HalResource}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.apprenticeshiplevy.connectors.DesConnector
import uk.gov.hmrc.apprenticeshiplevy.data.api.EmploymentReference
import uk.gov.hmrc.apprenticeshiplevy.utils.DecodePath._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

trait EmprefController extends DesController {
  def desConnector: DesConnector

  def declarationsUrl(empref: EmploymentReference): String

  def fractionsUrl(empref: EmploymentReference): String

  def employmentCheckUrl(empref: EmploymentReference): String

  def emprefUrl(empref: EmploymentReference): String

  // Hook to allow post-processing of the links, specifically for sandbox handling
  def processLink(l: HalLink): HalLink = identity(l)

  // scalastyle:off
  def empref(ref: EmploymentReference) = withValidAcceptHeader.async { implicit request =>
  // scalastyle:on
    desConnector.designatoryDetails(ref.empref).map { details =>
      val hal = prepareLinks(ref)
      ok(hal.copy(state = Json.toJson(details).as[JsObject]))
    }.recover(desErrorHandler)
  }

  private[controllers] def prepareLinks(empref: EmploymentReference): HalResource = {
    //links will be url encoded so need to decode them before sending back
    val links = Seq(
        selfLink(decodeAnyDoubleEncoding(emprefUrl(empref))),
        HalLink("declarations", decodeAnyDoubleEncoding(declarationsUrl(empref))),
        HalLink("fractions", decodeAnyDoubleEncoding(fractionsUrl(empref))),
        HalLink("employment-check", decodeAnyDoubleEncoding(employmentCheckUrl(empref)))
      )

    Hal.linksSeq(links.map(processLink))
  }
}
