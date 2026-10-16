package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.FourTranslationsTestData

@InternalController(["internal/e2e-data/four-translations"])
class FourTranslationsE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = FourTranslationsTestData().root
}
