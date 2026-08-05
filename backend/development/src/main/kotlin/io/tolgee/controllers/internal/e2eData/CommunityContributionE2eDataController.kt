package io.tolgee.controllers.internal.e2eData

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.CommunityContributionE2eData
import io.tolgee.service.security.UserAccountService
import org.springframework.beans.factory.annotation.Autowired

@InternalController(["internal/e2e-data/community-contribution"])
class CommunityContributionE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var userAccountService: UserAccountService

  @Autowired
  private lateinit var tolgeeProperties: TolgeeProperties

  override val testData: TestDataBuilder
    get() = CommunityContributionE2eData().root

  override fun afterTestDataStored(data: TestDataBuilder) {
    val project =
      data.data.projects.single {
        it.self.name == CommunityContributionE2eData.PUBLIC_PROJECT_NAME
      }
    val adminUsername = tolgeeProperties.authentication.initialUsername
    val adminId =
      userAccountService.findActive(adminUsername)?.id
        ?: throw IllegalStateException("No active account for initial username '$adminUsername'")
    recordContributorActivity(project.self.id, adminId)
  }
}
