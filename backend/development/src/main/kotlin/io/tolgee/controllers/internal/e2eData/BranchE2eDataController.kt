package io.tolgee.controllers.internal.e2eData

import io.tolgee.component.CurrentDateProvider
import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.BranchTestData
import org.springframework.beans.factory.annotation.Autowired

@InternalController(["internal/e2e-data/branch"])
class BranchE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var currentDateProvider: CurrentDateProvider

  override val testData: TestDataBuilder
    get() = BranchTestData(currentDateProvider).root
}
