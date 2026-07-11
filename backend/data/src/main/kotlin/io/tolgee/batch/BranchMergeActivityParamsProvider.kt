package io.tolgee.batch

import io.tolgee.activity.PublicParamsProvider
import io.tolgee.activity.data.EntityDescriptionRef
import io.tolgee.model.branching.BranchMerge
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class BranchMergeActivityParamsProvider(
  private val entityManager: EntityManager,
) : PublicParamsProvider {
  @Suppress("UNCHECKED_CAST")
  override fun provide(revisionIds: List<Long>): Map<Long, Any?> {
    val modifiedRows =
      entityManager
        .createQuery(
          """
          select me.activityRevision.id, me.describingRelations
          from ActivityModifiedEntity me
          where me.activityRevision.id in :revisionIds
            and me.entityClass = :entityClass
          """.trimIndent(),
        ).setParameter("revisionIds", revisionIds)
        .setParameter("entityClass", BranchMerge::class.simpleName)
        .resultList as List<Array<Any?>>

    if (modifiedRows.isEmpty()) return emptyMap()

    val describingRows =
      entityManager
        .createQuery(
          """
          select ade.activityRevision.id, ade.entityId, ade.data
          from ActivityDescribingEntity ade
          where ade.activityRevision.id in :revisionIds
            and ade.entityClass = :branchClass
          """.trimIndent(),
        ).setParameter("revisionIds", revisionIds)
        .setParameter("branchClass", "Branch")
        .resultList as List<Array<Any?>>

    val describingIndex =
      describingRows.associateBy { (it[0] as Long) to (it[1] as Long) }

    return modifiedRows.associate { row ->
      val revisionId = row[0] as Long
      val relations = row[1] as? Map<String, EntityDescriptionRef> ?: emptyMap()

      val sourceName =
        relations["sourceBranch"]?.let {
          val data = describingIndex[revisionId to it.entityId]?.get(2) as? Map<String, Any?>
          data?.get("name")
        }
      val targetName =
        relations["targetBranch"]?.let {
          val data = describingIndex[revisionId to it.entityId]?.get(2) as? Map<String, Any?>
          data?.get("name")
        }

      revisionId to mapOf("source" to sourceName, "target" to targetName)
    }
  }
}
