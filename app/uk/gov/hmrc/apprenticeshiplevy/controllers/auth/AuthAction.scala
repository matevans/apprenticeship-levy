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

package uk.gov.hmrc.apprenticeshiplevy.controllers.auth

import com.google.inject.{ImplementedBy, Inject}
import play.api.Configuration
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.Results.Status
import play.api.mvc._
import uk.gov.hmrc.apprenticeshiplevy.config.WSHttp
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(override val authConnector: MicroserviceAuthConnector)(implicit ec: ExecutionContext)
  extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)

    authorised(ConfidenceLevel.L50 and Enrolment("IR-PAYE")).retrieve(Retrievals.authorisedEnrolments) {

      case Enrolments(enrolments) => {
        enrolments.find(_.key == "IR-PAYE").map {
          enrolment =>
            val taxOfficeNumber = enrolment.identifiers.find(id => id.key == "TaxOfficeNumber").map(_.value)
            val taxOfficeReference = enrolment.identifiers.find(id => id.key == "TaxOfficeReference").map(_.value)

            (taxOfficeNumber, taxOfficeReference) match {
              case (Some(number), Some(reference)) =>
                block(AuthenticatedRequest(EmpRef(number, reference), request))
              case _ => Future.successful(Status(UNAUTHORIZED))
            }
        }.getOrElse(Future.successful(Status(UNAUTHORIZED)))
      }
    } recover {
      case ex: NoActiveSession =>
        Status(UNAUTHORIZED)
    }
  }
}

case class AuthenticatedRequest[A](empRef: EmpRef, request:Request[A]) extends WrappedRequest[A](request)

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest]

class MicroserviceAuthConnector @Inject()(val http: WSHttp, configuration: Configuration) extends PlayAuthConnector {

  val host = configuration.getString("microservice.services.auth.host").get
  val port = configuration.getString("microservice.services.auth.port").get

  override val serviceUrl: String = s"http://$host:$port"

}
