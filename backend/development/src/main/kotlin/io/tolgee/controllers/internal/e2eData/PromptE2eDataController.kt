package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.PromptTestData

@InternalController(["internal/e2e-data/prompt"])
class PromptE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() {
      val data = PromptTestData()
      data.addGlossary()
      return data.root
    }
}
