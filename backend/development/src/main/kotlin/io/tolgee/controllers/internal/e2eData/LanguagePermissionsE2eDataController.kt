package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/language-permissions"])
class LanguagePermissionsE2eDataController() : AbstractE2eDataController() {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData() {
    testDataService.saveTestData(testData)
  }

  override val testData: TestDataBuilder
    get() = LanguagePermissionsTestData().root
}
