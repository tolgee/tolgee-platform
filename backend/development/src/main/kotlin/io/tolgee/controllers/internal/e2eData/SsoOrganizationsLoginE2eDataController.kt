package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.SsoOrganizationsLoginTestData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/sso-organizations-login"])
class SsoOrganizationsLoginE2eDataController : AbstractE2eDataController() {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData() {
    testDataService.saveTestData(testData)
  }

  override val testData: TestDataBuilder
    get() = SsoOrganizationsLoginTestData().root
}
