package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import java.util.Date

@InternalController(["internal/e2e-data/members-community"])
class MembersCommunityE2eDataController : AbstractE2eDataController() {
  private lateinit var currentTestData: ContributorsTestData

  override val testData: TestDataBuilder
    get() = ContributorsTestData().also { currentTestData = it }.root

  override fun afterTestDataStored(data: TestDataBuilder) {
    listOf(currentTestData.contributor.id, currentTestData.contributor2.id).forEach { authorId ->
      recordContributorActivity(currentTestData.publicProject.id, authorId, FIRST_CONTRIBUTION_AT)
      recordContributorActivity(currentTestData.publicProject.id, authorId, LAST_CONTRIBUTION_AT)
    }
    recordContributorActivity(currentTestData.project.id, currentTestData.contributor.id, LAST_CONTRIBUTION_AT)
  }

  companion object {
    private val FIRST_CONTRIBUTION_AT = Date(1_560_600_000_000)
    private val LAST_CONTRIBUTION_AT = Date(1_623_758_400_000)
  }
}
