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

package uk.gov.hmrc.apprenticeshiplevy.controllers.live

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.apprenticeshiplevy.connectors.EpayeConnector
import uk.gov.hmrc.apprenticeshiplevy.controllers.EmprefController

object LiveEmprefController extends EmprefController {
  override def emprefUrl(empref: String): String = routes.LiveEmprefController.empref(empref).url

  override def declarationsUrl(empref: String): String = routes.LiveLevyDeclarationController.declarations(empref, None).url

  override def fractionsUrl(empref: String): String = routes.LiveFractionsController.fractions(empref, None).url

  override def epayeConnector: EpayeConnector = ???

  override def empref(empref: String): Action[AnyContent] = Action(NotImplemented)
}
