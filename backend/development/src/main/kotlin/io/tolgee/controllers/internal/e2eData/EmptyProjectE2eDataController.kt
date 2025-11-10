package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.EmptyProjectTestData

@InternalController(["internal/e2e-data/empty-project"])
class EmptyProjectE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = EmptyProjectTestData().root
}
