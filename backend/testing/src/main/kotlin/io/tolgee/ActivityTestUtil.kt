package io.tolgee

import io.tolgee.model.activity.ActivityRevision
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class ActivityTestUtil(
  private val entityManager: EntityManager,
) {
  fun getLastRevision(): ActivityRevision? {
    return entityManager
      .createQuery(
        """
        from ActivityRevision ar order by ar.id desc limit 1
        """.trimMargin(),
        ActivityRevision::class.java,
      ).singleResult
  }
}
