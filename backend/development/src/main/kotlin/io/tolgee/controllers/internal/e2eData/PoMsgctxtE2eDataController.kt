package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.PoMsgctxtTestData

@InternalController(["internal/e2e-data/po-msgctxt"])
class PoMsgctxtE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = PoMsgctxtTestData().root
}
