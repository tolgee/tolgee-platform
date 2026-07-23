package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.CommunityContributionE2eData
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.service.security.UserAccountService
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired

@InternalController(["internal/e2e-data/community-contribution"])
class CommunityContributionE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var userAccountLookup: UserAccountService

  private lateinit var data: CommunityContributionE2eData

  override val testData: TestDataBuilder
    get() {
      data = CommunityContributionE2eData()
      return data.root
    }

  // Seed a contribution by the seeded platform admin (the default e2e login user) on a public project it
  // is not a member of, so the gated "Community translation" switcher entry renders for that user.
  override fun afterTestDataStored(data: TestDataBuilder) {
    val adminId = userAccountLookup.findActive(ADMIN_USERNAME)?.id ?: return
    entityManager.persist(
      ActivityRevision().apply {
        projectId = this@CommunityContributionE2eDataController.data.publicProject.id
        authorId = adminId
      },
    )
    entityManager.flush()
  }

  companion object {
    private const val ADMIN_USERNAME = "admin"
  }
}
