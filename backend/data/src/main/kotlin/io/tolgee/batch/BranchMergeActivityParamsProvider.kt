package io.tolgee.batch

import io.mockk.InternalPlatformDsl.toStr
import io.tolgee.activity.PublicParamsProvider
import io.tolgee.model.branching.BranchMerge
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class BranchMergeActivityParamsProvider(
  private val entityManager: EntityManager,
) : PublicParamsProvider {
  override fun provide(revisionIds: List<Long>): Map<Long, Any?> {
    return entityManager
      .createQuery(
        """select ar.id, bm.sourceBranch.name, bm.targetBranch.name from ActivityRevision ar 
          join ar.modifiedEntities me
          join BranchMerge bm on bm.id = me.entityId
          where ar.id in :revisionIds and me.entityClass = :class      
    """,
        Array<Any?>::class.java,
      ).setParameter("revisionIds", revisionIds)
      .setParameter("class", BranchMerge::class.toStr())
      .resultList
      .associate {
        ((it[0] as Long to mapOf("source" to it[1], "target" to it[2])))
      }
  }
}
