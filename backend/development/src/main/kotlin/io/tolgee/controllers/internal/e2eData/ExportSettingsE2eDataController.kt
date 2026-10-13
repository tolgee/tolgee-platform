package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ExportSettingsTestData

@InternalController(["internal/e2e-data/export-settings"])
class ExportSettingsE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = ExportSettingsTestData().root
}
