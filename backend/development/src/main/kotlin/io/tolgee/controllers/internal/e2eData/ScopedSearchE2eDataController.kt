package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ScopedSearchTestData

@InternalController(["internal/e2e-data/scoped-search"])
class ScopedSearchE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = ScopedSearchTestData().root
}
