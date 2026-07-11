package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.dataImport.ImportNamespacesTestData

@InternalController(["internal/e2e-data/import-namespaces"])
class ImportNamespacesE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = ImportNamespacesTestData().root
}
