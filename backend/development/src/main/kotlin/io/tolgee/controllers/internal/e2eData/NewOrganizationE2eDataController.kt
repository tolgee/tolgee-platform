package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.OrganizationTestData

@InternalController(["internal/e2e-data/organization-new"])
class NewOrganizationE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = OrganizationTestData().root
}
