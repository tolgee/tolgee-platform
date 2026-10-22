package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.CommunityStatsE2eData
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource

@InternalController(["internal/e2e-data/community-stats"])
class CommunityStatsE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  override val testData: TestDataBuilder
    get() = CommunityStatsE2eData().root

  override fun afterTestDataStored(data: TestDataBuilder) {
    val projectIds = data.data.projects.map { it.self.id }
    backdateSuggestions(projectIds)
    rebuildCountersOfEveryProject(deletingFirst = projectIds)
  }

  private fun backdateSuggestions(projectIds: List<Long>) {
    entityManager
      .createNativeQuery(
        """
        update translation_suggestion
        set created_at = now() - ((id * 7919) % $SPREAD_DAYS) * interval '1 day',
            updated_at = least(now(), now() - ((id * 7919) % $SPREAD_DAYS - id % 4) * interval '1 day')
        where project_id in (:projectIds)
        """,
      ).setParameter("projectIds", projectIds)
      .executeUpdate()
  }

  private fun rebuildCountersOfEveryProject(deletingFirst: List<Long>) {
    entityManager
      .createNativeQuery("delete from community_contribution_day where project_id in (:projectIds)")
      .setParameter("projectIds", deletingFirst)
      .executeUpdate()
    entityManager
      .createNativeQuery(
        ClassPathResource("db/changelog/communityContributionDayBackfill.sql").getContentAsString(Charsets.UTF_8),
      ).executeUpdate()
  }

  companion object {
    private const val SPREAD_DAYS = 70
  }
}
