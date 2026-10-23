package io.tolgee.ee.service.aiReview

import io.tolgee.ee.service.aiReview.matching.KeyScope
import jakarta.persistence.EntityManager
import jakarta.persistence.Tuple
import org.springframework.stereotype.Component

@Component
class AiReviewKeyScopeQuery(
  private val entityManager: EntityManager,
) {
  data class ScopeGroup(
    val scope: KeyScope,
    val keyCount: Long,
  )

  fun forEachKeyScope(
    projectId: Long,
    keyIds: Collection<Long>?,
    defaultBranchName: String,
    fn: (KeyScope) -> Unit,
  ) {
    val keyFilter = if (keyIds == null) "" else "and k.id in :keyIds"
    val query =
      entityManager
        .createQuery(
          """
          select k.id, n.name, b.name, t.name from Key k
          left join k.namespace n
          left join k.branch b
          left join k.keyMeta km
          left join km.tags t
          where k.project.id = :projectId and k.deletedAt is null and (b is null or b.deletedAt is null)
          $keyFilter
          order by k.id
          """,
          Tuple::class.java,
        ).setParameter("projectId", projectId)
    keyIds?.let { query.setParameter("keyIds", it) }

    var current: KeyScope? = null
    query.resultStream.forEach { row ->
      val keyId = row.get(0, java.lang.Long::class.java).toLong()
      val tag = row.get(3, String::class.java)
      val previous = current
      if (previous != null && previous.keyId == keyId) {
        current = previous.copy(tags = previous.tags + listOfNotNull(tag))
        return@forEach
      }
      previous?.let(fn)
      current =
        KeyScope(
          keyId = keyId,
          namespace = row.get(1, String::class.java) ?: "",
          branchName = row.get(2, String::class.java) ?: defaultBranchName,
          tags = setOfNotNull(tag),
        )
    }
    current?.let(fn)
  }

  fun groupKeyScopes(
    projectId: Long,
    keyIds: Collection<Long>?,
    defaultBranchName: String,
  ): List<ScopeGroup> {
    val keyFilter = if (keyIds == null) "" else "and k.id in (:keyIds)"
    val query =
      entityManager
        .createNativeQuery(
          """
          select coalesce(n.name, '') as namespace_name,
                 coalesce(b.name, :defaultBranch) as branch_name,
                 coalesce(
                   (select string_agg(tg.name, chr(31) order by tg.name)
                    from key_meta km
                    join key_meta_tags kmt on kmt.key_metas_id = km.id
                    join tag tg on tg.id = kmt.tags_id
                    where km.key_id = k.id),
                   ''
                 ) as tag_names,
                 count(*) as key_count
          from key k
          left join namespace n on n.id = k.namespace_id
          left join branch b on b.id = k.branch_id
          where k.project_id = :projectId and k.deleted_at is null and (b.id is null or b.deleted_at is null)
          $keyFilter
          group by 1, 2, 3
          """,
          Tuple::class.java,
        ).setParameter("projectId", projectId)
        .setParameter("defaultBranch", defaultBranchName)
    keyIds?.let { query.setParameter("keyIds", it) }

    @Suppress("UNCHECKED_CAST")
    return (query.resultList as List<Tuple>).map { row ->
      val tagNames = row.get("tag_names", String::class.java)
      ScopeGroup(
        scope =
          KeyScope(
            keyId = 0,
            namespace = row.get("namespace_name", String::class.java),
            branchName = row.get("branch_name", String::class.java),
            tags = tagNames.split(TAG_SEPARATOR).filter { it.isNotEmpty() }.toSet(),
          ),
        keyCount = (row.get("key_count") as Number).toLong(),
      )
    }
  }

  companion object {
    private const val TAG_SEPARATOR = '\u001F'
  }
}
