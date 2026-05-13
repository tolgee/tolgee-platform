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
        from ActivityRevision ar left join fetch ar.modifiedEntities order by ar.id desc limit 1
        """.trimMargin(),
        ActivityRevision::class.java,
      ).resultList
      .firstOrNull()
  }

  fun findRevisionsAfter(afterId: Long): List<ActivityRevision> {
    return entityManager
      .createQuery(
        """
        select distinct ar from ActivityRevision ar left join fetch ar.modifiedEntities
          where ar.id > :afterId order by ar.id asc
        """.trimMargin(),
        ActivityRevision::class.java,
      ).setParameter("afterId", afterId)
      .resultList
  }
}
