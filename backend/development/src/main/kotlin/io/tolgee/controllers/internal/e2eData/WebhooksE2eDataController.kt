package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.WebhooksE2eTestData

@InternalController(["internal/e2e-data/webhooks"])
class WebhooksE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() {
      val data = WebhooksE2eTestData()
      return data.root
    }
}
