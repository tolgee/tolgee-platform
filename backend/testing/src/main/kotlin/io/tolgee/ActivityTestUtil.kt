package io.tolgee

import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class ActivityTestUtil(
  private val entityManager: EntityManager,
) {
  fun getLastRevision(projectId: Long): ActivityRevision? {
    return entityManager
      .createQuery(
        """
        from ActivityRevision ar where ar.projectId = :projectId order by ar.id desc limit 1
        """.trimMargin(),
        ActivityRevision::class.java,
      ).setParameter("projectId", projectId)
      .singleResult
  }
}
