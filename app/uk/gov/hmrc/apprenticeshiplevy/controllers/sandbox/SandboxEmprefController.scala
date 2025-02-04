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

package uk.gov.hmrc.apprenticeshiplevy.controllers.sandbox

import com.google.inject.Inject
import org.joda.time.LocalDate
import play.api.hal.HalLink
import uk.gov.hmrc.apprenticeshiplevy.config.AppContext
import uk.gov.hmrc.apprenticeshiplevy.connectors.SandboxDesConnector
import uk.gov.hmrc.apprenticeshiplevy.controllers.EmprefController
import uk.gov.hmrc.apprenticeshiplevy.data.api.{EmploymentReference, Nino}

class SandboxEmprefController @Inject()(val desConnector: SandboxDesConnector) extends EmprefController with SandboxLinkHelper {
  override val env = AppContext.env

  override def emprefUrl(empref: EmploymentReference): String = routes.SandboxEmprefController.empref(empref).url

  override def declarationsUrl(empref: EmploymentReference): String = routes.SandboxLevyDeclarationController.declarations(empref, None, None).url

  override def fractionsUrl(empref: EmploymentReference): String = routes.SandboxFractionsController.fractions(empref, None, None).url

  override def employmentCheckUrl(empref: EmploymentReference): String = {
    routes.SandboxEmploymentCheckController.check(empref, Nino("nino"), new LocalDate, new LocalDate)
      .url.replaceAll("\\?.*", "").replaceAll("nino", "{nino}")
  }

  override def processLink(l: HalLink): HalLink = stripSandboxForNonDev(l)

}
