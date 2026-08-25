package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ProjectSwitchingTestData

@InternalController(["internal/e2e-data/project-switching"])
class ProjectSwitchingE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = ProjectSwitchingTestData().root
}
