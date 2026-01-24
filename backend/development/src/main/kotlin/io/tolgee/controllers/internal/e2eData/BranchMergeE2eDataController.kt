package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.BranchMergeTestData

@InternalController(["internal/e2e-data/branch-merge"])
class BranchMergeE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = BranchMergeTestData().root
}
