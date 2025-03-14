package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.component.CurrentDateProvider
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.AuthProviderChangeEeTestData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/auth-provider-change"])
@Transactional
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
