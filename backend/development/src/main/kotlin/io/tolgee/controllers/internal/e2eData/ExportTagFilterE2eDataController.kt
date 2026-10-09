package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ExportTagAndNamespaceFilterTestData

@InternalController(["internal/e2e-data/export-tag-filter"])
class ExportTagFilterE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = ExportTagAndNamespaceFilterTestData().root
}
