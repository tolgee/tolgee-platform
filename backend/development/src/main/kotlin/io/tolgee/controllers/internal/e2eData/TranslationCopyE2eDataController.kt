package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.TranslationCopyTestData

@InternalController(["internal/e2e-data/translation-copy"])
class TranslationCopyE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = TranslationCopyTestData().root
}
