package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData

@InternalController(["internal/e2e-data/batch-jobs"])
class BatchJobsE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() {
      val data = BatchJobsTestData()
      data.addTranslationOperationData()
      return data.root
    }
}
