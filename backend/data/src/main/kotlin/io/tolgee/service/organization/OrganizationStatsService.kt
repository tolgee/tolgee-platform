package io.tolgee.service.organization

import io.tolgee.repository.OrganizationRepository.Companion.ALL_USERS_IN_ORGANIZATION_QUERY_TO_COUNT_USAGE_FOR
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
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

  fun getTranslationCount(organizationId: Long): Long = getTranslationAndWordCount(organizationId).translations

  fun getWordCount(organizationId: Long): Long = getTranslationAndWordCount(organizationId).words

  fun getTranslationAndWordCount(organizationId: Long): TranslationAndWordCount =
    entityManager
      .createNativeQuery(countedTranslationsQuery(ORGANIZATION_SCOPE))
      .setParameter("organizationId", organizationId)
      .toTranslationAndWordCount()

  fun countAllWordsOnInstance(): Long =
    entityManager
      .createNativeQuery(countedTranslationsQuery(INSTANCE_SCOPE))
      .toTranslationAndWordCount()
      .words

  fun getKeyCount(organizationId: Long): Long {
    return entityManager
      .createNativeQuery(countedKeysQuery(ORGANIZATION_SCOPE))
      .setParameter("organizationId", organizationId)
      .singleResult
      .let { (it as Number).toLong() }
  }

  private fun Query.toTranslationAndWordCount(): TranslationAndWordCount {
    val row = singleResult as Array<*>
    return TranslationAndWordCount(
      translations = (row[0] as Number).toLong(),
      words = (row[1] as Number).toLong(),
    )
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

    private data class Scope(
      val join: String,
      val filter: String,
    )

    /**
     * A project's counted keys can repeat a name only across slots (no branch, or one counted branch):
     * the unique indexes key_project_branch_name_(no_)ns forbid it within one. Projects with at most
     * one slot are counted per row; only the rest pay for grouping by name. Revisit if those indexes
     * change.
     */
    private fun countedKeysCte(scope: Scope): String =
      """
      with counted_projects as materialized (
        select p.id, p.use_branching,
          (
            exists (
              select 1 from key k
              where k.project_id = p.id and coalesce(k.branch_id, -1) = -1
                and k.namespace_id is null and k.deleted_at is null
            )
            or exists (
              select 1 from key k
              where k.project_id = p.id and coalesce(k.branch_id, -1) = -1
                and k.namespace_id is not null and k.deleted_at is null
            )
          )::int
          + (
            select count(*) from branch b
            where b.project_id = p.id and b.deleted_at is null and (p.use_branching = true or b.is_default = true)
              and exists (select 1 from key k where k.branch_id = b.id and k.deleted_at is null)
          ) as key_slots
        from project p
        ${scope.join}
        where p.deleted_at is null
          ${scope.filter}
      ),
      counted_keys as not materialized (
        select k.id, k.project_id, k.name, k.namespace_id, cp.key_slots
        from counted_projects cp
        join key k on k.project_id = cp.id
        left join branch b on b.id = k.branch_id
        where k.deleted_at is null
          and (k.branch_id is null or b.deleted_at is null)
          and (cp.use_branching = true or k.branch_id is null or b.is_default = true)
      )
      """.trimIndent()

    private fun countedKeysQuery(scope: Scope): String =
      """
      ${countedKeysCte(scope)}
      select
        (select count(*) from counted_keys where key_slots <= 1)
        + (
          select count(*) from (
            select distinct project_id, name, namespace_id from counted_keys where key_slots > 1
          ) grouped
        )
      """.trimIndent()

    private fun countedTranslationsQuery(scope: Scope): String =
      """
      ${countedKeysCte(scope)},
      counted_languages as materialized (
        select l.id from language l
        where l.project_id in (select id from counted_projects) and l.deleted_at is null
      ),
      single_slot as (
        select count(*) as translations, coalesce(sum(t.word_count), 0) as words
        from counted_keys ck
        join translation t on t.key_id = ck.id and t.text is not null and t.text <> ''
        join counted_languages l on l.id = t.language_id
        where ck.key_slots <= 1
      ),
      multi_slot_keys as materialized (
        select id, project_id, name, namespace_id from counted_keys where key_slots > 1
      ),
      multi_slot_translations as materialized (
        select mk.project_id, mk.name, mk.namespace_id, t.language_id, coalesce(t.word_count, 0) as word_count
        from multi_slot_keys mk
        join translation t on t.key_id = mk.id and t.text is not null and t.text <> ''
      ),
      multi_slot as (
        select count(*) as translations, coalesce(sum(grouped.max_word_count), 0) as words
        from (
          select max(mt.word_count) as max_word_count
          from multi_slot_translations mt
          where mt.language_id in (select id from counted_languages)
          group by mt.project_id, mt.name, mt.namespace_id, mt.language_id
        ) grouped
      )
      select single_slot.translations + multi_slot.translations, single_slot.words + multi_slot.words
      from single_slot, multi_slot
      """.trimIndent()
  }
}
