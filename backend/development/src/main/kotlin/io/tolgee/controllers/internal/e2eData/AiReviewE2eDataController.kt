package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.AiReviewE2eTestData

@InternalController(["internal/e2e-data/ai-review"])
class AiReviewE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = AiReviewE2eTestData().root
}
