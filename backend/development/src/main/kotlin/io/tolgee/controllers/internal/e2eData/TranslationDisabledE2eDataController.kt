package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.KeyLanguageDisablingTestData

@InternalController(["internal/e2e-data/translation-disabled"])
class TranslationDisabledE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = KeyLanguageDisablingTestData().root
}
