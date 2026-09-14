package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.TranslationViewsTestData

@InternalController(["internal/e2e-data/translation-views"])
class TranslationViewsE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = TranslationViewsTestData().root
}
