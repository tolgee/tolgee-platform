package io.tolgee.controllers.internal.e2eData

import io.tolgee.component.CurrentDateProvider
import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date

@InternalController(["internal/e2e-data/members-community"])
class MembersCommunityE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var currentDateProvider: CurrentDateProvider

  private lateinit var data: ContributorsTestData

  override val testData: TestDataBuilder
    get() = ContributorsTestData().also { data = it }.root

  override fun afterTestDataStored(data: TestDataBuilder) {
    try {
      listOf(this.data.contributor.id, this.data.contributor2.id).forEach { authorId ->
        recordActivity(this.data.publicProject.id, authorId, FIRST_CONTRIBUTION_AT)
        recordActivity(this.data.publicProject.id, authorId, LAST_CONTRIBUTION_AT)
      }
    } finally {
      currentDateProvider.forcedDate = null
    }
  }

  private fun recordActivity(
    projectId: Long,
    authorId: Long,
    at: Date,
  ) {
    currentDateProvider.forcedDate = at
    entityManager.persist(
      ActivityRevision().apply {
        this.projectId = projectId
        this.authorId = authorId
      },
    )
    entityManager.flush()
  }

  companion object {
    private val FIRST_CONTRIBUTION_AT = Date(1_560_600_000_000)
    private val LAST_CONTRIBUTION_AT = Date(1_623_758_400_000)
  }
}
