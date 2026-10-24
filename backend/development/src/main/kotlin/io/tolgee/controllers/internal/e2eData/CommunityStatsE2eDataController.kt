package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.CommunityActivityReplayer
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.CommunityStatsE2eData
import io.tolgee.development.testDataBuilder.data.CommunityStatsTestData
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired

@InternalController(["internal/e2e-data/community-stats"])
class CommunityStatsE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  override val testData: TestDataBuilder
    get() = CommunityStatsE2eData().root

  override fun afterTestDataStored(data: TestDataBuilder) {
    val projectIds = data.data.projects.map { it.self.id }
    backdate("translation_suggestion", "project_id in (:projectIds)", projectIds)
    backdate("translation", "key_id in (select id from key where project_id in (:projectIds))", projectIds)
    backdate(
      "translation_comment",
      "translation_id in (select t.id from translation t join key k on k.id = t.key_id where k.project_id in (:projectIds))",
      projectIds,
    )
    val maintainerId =
      data.data.userAccounts
        .single { it.self.username == CommunityStatsTestData.OWNER_USERNAME }
        .self.id
    CommunityActivityReplayer.replay(entityManager, projectIds, maintainerId)
  }

  private fun backdate(
    table: String,
    where: String,
    projectIds: List<Long>,
  ) {
    entityManager
      .createNativeQuery(
        """
        update $table
        set created_at = now() - ((id * 7919) % $SPREAD_DAYS) * interval '1 day',
            updated_at = least(now(), now() - ((id * 7919) % $SPREAD_DAYS - id % 4) * interval '1 day')
        where $where
        """,
      ).setParameter("projectIds", projectIds)
      .executeUpdate()
  }

  companion object {
    private const val SPREAD_DAYS = 70
  }
}
