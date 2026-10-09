package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.CopyTranslationTestData

@InternalController(["internal/e2e-data/copy-translation"])
class CopyTranslationE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = CopyTranslationTestData().root
}
