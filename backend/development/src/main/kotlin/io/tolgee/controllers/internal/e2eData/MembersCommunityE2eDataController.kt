package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ContributorsTestData

@InternalController(["internal/e2e-data/members-community"])
class MembersCommunityE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() = ContributorsTestData(withE2eContributions = true).root
}
