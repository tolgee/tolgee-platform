package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ContentDeliveryConfigBranchingTestData

@InternalController(["internal/e2e-data/content-delivery-branching"])
class ContentDeliveryBranchingE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() {
      val data = ContentDeliveryConfigBranchingTestData()
      return data.root
    }
}
