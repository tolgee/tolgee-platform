package io.tolgee.service.organization

import io.tolgee.repository.OrganizationRepository.Companion.ALL_USERS_IN_ORGANIZATION_QUERY_TO_COUNT_USAGE_FOR
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

@Service
class OrganizationStatsService(
  private val entityManager: EntityManager,
) {
  fun getProjectLanguageCount(projectId: Long): Long {
    return entityManager
      .createQuery(
        """
        select count(l) from Language l 
        where l.project.id = :projectId and l.project.deletedAt is null
        """.trimIndent(),
      ).setParameter("projectId", projectId)
      .singleResult as Long
  }

  fun getProjectKeyCount(projectId: Long): Long {
    return entityManager
      .createQuery(
        """
        select count(distinct k.name, k.namespace) from Key k
        where k.project.id = :projectId and k.project.deletedAt is null and k.deletedAt is null
        """.trimIndent(),
      ).setParameter("projectId", projectId)
      .singleResult as Long
  }

  fun getSeatCountToCountSeats(organizationId: Long): Long {
    return entityManager
      .createQuery(
        """
        select count(distinct ua.id) $ALL_USERS_IN_ORGANIZATION_QUERY_TO_COUNT_USAGE_FOR
        """.trimIndent(),
      ).setParameter("organizationId", organizationId)
      .singleResult as Long
  }

  fun getTranslationCount(organizationId: Long): Long {
    return entityManager
      .createNativeQuery(countedTranslationsQuery(ORGANIZATION_SCOPE, TRANSLATION_COUNT_SELECT))
      .setParameter("organizationId", organizationId)
      .singleResult
      .let { (it as Number).toLong() }
  }

  fun getWordCount(organizationId: Long): Long {
    return entityManager
      .createNativeQuery(countedTranslationsQuery(ORGANIZATION_SCOPE, WORD_COUNT_SELECT))
      .setParameter("organizationId", organizationId)
      .singleResult
      .let { (it as Number).toLong() }
  }

  fun getTranslationAndWordCount(organizationId: Long): TranslationAndWordCount {
    val row =
      entityManager
        .createNativeQuery(countedTranslationsQuery(ORGANIZATION_SCOPE, TRANSLATION_AND_WORD_COUNT_SELECT))
        .setParameter("organizationId", organizationId)
        .singleResult as Array<*>
    return TranslationAndWordCount(
      translations = (row[0] as Number).toLong(),
      words = (row[1] as Number).toLong(),
    )
  }

  fun countAllWordsOnInstance(): Long {
    return entityManager
      .createNativeQuery(countedTranslationsQuery(INSTANCE_SCOPE, WORD_COUNT_SELECT))
      .singleResult
      .let { (it as Number).toLong() }
  }

  fun getKeyCount(organizationId: Long): Long {
    return entityManager
      .createNativeQuery(countedKeysQuery(ORGANIZATION_SCOPE))
      .setParameter("organizationId", organizationId)
      .singleResult
      .let { (it as Number).toLong() }
  }

  data class TranslationAndWordCount(
    val translations: Long,
    val words: Long,
  )

  companion object {
    private val ORGANIZATION_SCOPE =
      Scope(join = "", filter = "and p.organization_owner_id = :organizationId")

    private val INSTANCE_SCOPE =
      Scope(join = "join organization o on o.id = p.organization_owner_id and o.deleted_at is null", filter = "")

    private const val TRANSLATION_COUNT_SELECT = "count(*)"

    private const val WORD_COUNT_SELECT = "coalesce(sum(grouped.max_word_count), 0)"

    private const val TRANSLATION_AND_WORD_COUNT_SELECT = "count(*), coalesce(sum(grouped.max_word_count), 0)"

    private data class Scope(
      val join: String,
      val filter: String,
    )

    private fun countedKeysQuery(scope: Scope): String =
      """
      ${countedKeysCte(scope, materialized = false)}
      select count(*) from (
        select distinct ck.project_id, ck.name, ck.namespace_id from counted_keys ck
      ) grouped
      """.trimIndent()

    private fun countedKeysCte(
      scope: Scope,
      materialized: Boolean = true,
    ): String =
      """
      with counted_keys as ${if (materialized) "materialized " else ""}(
        select k.id, k.project_id, k.name, k.namespace_id
        from key k
        join project p on p.id = k.project_id and p.deleted_at is null
        ${scope.join}
        left join branch b on b.id = k.branch_id
        where k.deleted_at is null
          ${scope.filter}
          and (k.branch_id is null or b.deleted_at is null)
          and (p.use_branching = true or k.branch_id is null or b.is_default = true)
      )
      """.trimIndent()

    private fun countedTranslationsQuery(
      scope: Scope,
      select: String,
    ): String =
      """
      ${countedKeysCte(scope)},
      counted_translations as materialized (
        select ck.project_id, ck.name, ck.namespace_id, t.language_id,
               coalesce(t.word_count, 0) as word_count
        from counted_keys ck
        join translation t on t.key_id = ck.id
          and t.text is not null
          and t.text <> ''
      )
      select $select from (
        select max(ct.word_count) as max_word_count
        from counted_translations ct
        where exists (
          select 1 from language l
          where l.id = ct.language_id
            and l.deleted_at is null
        )
        group by ct.project_id, ct.name, ct.namespace_id, ct.language_id
      ) grouped
      """.trimIndent()
  }
}
