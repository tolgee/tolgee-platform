package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.CommunityContributionE2eData
import io.tolgee.service.security.UserAccountService
import org.springframework.beans.factory.annotation.Autowired

@InternalController(["internal/e2e-data/community-contribution"])
class CommunityContributionE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var userAccountService: UserAccountService

  private lateinit var currentTestData: CommunityContributionE2eData

  override val testData: TestDataBuilder
    get() = CommunityContributionE2eData().also { currentTestData = it }.root

  override fun afterTestDataStored(data: TestDataBuilder) {
    val adminId = userAccountService.findActive(ADMIN_USERNAME)?.id ?: return
    recordContributorActivity(currentTestData.publicProject.id, adminId)
  }

  companion object {
    private const val ADMIN_USERNAME = "admin"
  }
}
