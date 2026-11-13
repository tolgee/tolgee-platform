package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.NotificationsPocE2eTestData

@InternalController(["internal/e2e-data/notifications-poc"])
class NotificationsPocE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = NotificationsPocE2eTestData().root
}
