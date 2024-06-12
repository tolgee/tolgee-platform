package io.tolgee.service.key

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class TaggedKeyIdsProvider(
  private val entityManager: EntityManager,
) {
  fun getFilteredKeys(
    projectId: Long,
    filterTag: List<String>?,
    filterTagNot: List<String>?,
  ): List<Long> {
    val filterTagCondition =
      filterTag?.let {
        """
      km.id in (
        select km.id from key_meta km
        join key_meta_tags tk on km.id = tk.key_metas_id
        join public.tag t on tk.tags_id = t.id
        where t.name in :filterTag
     )
    """
      } ?: "1 = 1"

    val filterTagNotCondition =
      filterTagNot?.let {
        """
      (km.id is null or km.id not in (
        select km.id from key_meta km
        join key_meta_tags tk on km.id = tk.key_metas_id
        join public.tag t on tk.tags_id = t.id
        where t.name in :filterTagNot
     ))
    """
      } ?: "1 = 1"

    @Suppress("SqlSourceToSinkFlow")
    val query =
      entityManager.createNativeQuery(
        """
     select k.id from key k
     left join key_meta km on k.id = km.key_id
     where k.project_id = :projectId and $filterTagCondition and $filterTagNotCondition 
    """,
        Long::class.java,
      )

    query.setParameter("projectId", projectId)

    filterTag?.let {
      query.setParameter("filterTag", filterTag.applyWildcards(projectId))
    }

    filterTagNot?.let {
      query.setParameter("filterTagNot", filterTagNot.applyWildcards(projectId))
    }

    @Suppress("UNCHECKED_CAST")
    return query.resultList as List<Long>
  }

  fun Collection<String>.applyWildcards(projectId: Long): Collection<String> {
    return WildcardTagsProvider(entityManager).getTagsWithAppliedWildcards(projectId, this)
  }
}
