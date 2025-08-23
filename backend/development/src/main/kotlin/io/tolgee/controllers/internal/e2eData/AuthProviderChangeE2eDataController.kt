package io.tolgee.controllers.internal.e2eData

import io.tolgee.component.CurrentDateProvider
import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.AuthProviderChangeEeTestData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping

@InternalController(["internal/e2e-data/auth-provider-change"])
class AuthProviderChangeE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var currentDateProvider: CurrentDateProvider

  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData() {
    testDataService.saveTestData(testData)
  }

  override val testData: TestDataBuilder
    get() = AuthProviderChangeEeTestData(currentDateProvider.date).root
}
