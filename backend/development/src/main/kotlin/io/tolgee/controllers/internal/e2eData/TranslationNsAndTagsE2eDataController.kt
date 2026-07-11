package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.TranslationNsAndTagsData

@InternalController(["internal/e2e-data/ns-and-tags"])
class TranslationNsAndTagsE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = TranslationNsAndTagsData().root
}
