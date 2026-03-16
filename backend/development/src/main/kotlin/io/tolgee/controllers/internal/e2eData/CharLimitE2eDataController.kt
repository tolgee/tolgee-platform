package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.CharLimitTestData

@InternalController(["internal/e2e-data/char-limit"])
class CharLimitE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = CharLimitTestData().root
}
