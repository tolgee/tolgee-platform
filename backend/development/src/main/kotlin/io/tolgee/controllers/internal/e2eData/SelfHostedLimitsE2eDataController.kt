package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.SelfHostedLimitsTestData

@InternalController(["internal/e2e-data/self-hosted-limits"])
class SelfHostedLimitsE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = SelfHostedLimitsTestData().root
}
