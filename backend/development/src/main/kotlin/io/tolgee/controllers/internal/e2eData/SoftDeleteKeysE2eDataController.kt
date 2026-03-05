package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.SoftDeleteKeysTestData

@InternalController(["internal/e2e-data/soft-delete-keys"])
class SoftDeleteKeysE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = SoftDeleteKeysTestData().root
}
