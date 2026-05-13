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
      .createNativeQuery(
        """
        with org_keys as materialized (
          select k.id, k.project_id, k.name, k.namespace_id
          from key k
          join project p on p.id = k.project_id and p.deleted_at is null
          left join branch b on b.id = k.branch_id
          where p.organization_owner_id = :organizationId
            and k.deleted_at is null
            and (k.branch_id is null or b.deleted_at is null)
            and (p.use_branching = true or k.branch_id is null or b.is_default = true)
        ),
        org_translations as materialized (
          select ok.project_id, ok.name, ok.namespace_id, t.language_id
          from org_keys ok
          join translation t on t.key_id = ok.id
            and t.text is not null
            and t.text <> ''
        )
        select count(*) from (
          select distinct ot.project_id, ot.name, ot.namespace_id, ot.language_id
          from org_translations ot
          where exists (
            select 1 from language l
            where l.id = ot.language_id
              and l.deleted_at is null
          )
        ) sub
        """.trimIndent(),
      ).setParameter("organizationId", organizationId)
      .singleResult
      .let { (it as Number).toLong() }
  }

  fun getKeyCount(organizationId: Long): Long {
    return (
      entityManager
        .createNativeQuery(
          """
          select count(*) from (
              select distinct k.project_id, k.name, k.namespace_id
              from key k
              join project p on p.id = k.project_id and p.deleted_at is null
              left join branch b on b.id = k.branch_id
              where p.organization_owner_id = :organizationId
                and k.deleted_at is null
                and (k.branch_id is null or b.deleted_at is null)
                and (p.use_branching = true or k.branch_id is null or b.is_default = true)
          ) sub
          """.trimIndent(),
        ).setParameter("organizationId", organizationId)
        .singleResult as Number
    ).toLong()
  }

  /**
   * Counter contribution of a single project, applying the same branching rule as
   * [getKeyCount]. Used by edge-case handlers (e.g. project soft-delete, branching toggle)
   * to compute deltas to apply to the organization usage counter.
   *
   * Important: this query does NOT exclude soft-deleted projects, so it returns the
   * contribution that would be lost if the project were soft-deleted. Callers should
   * therefore use this before the soft-delete.
   */
  fun getProjectKeyContribution(projectId: Long): Long {
    return (
      entityManager
        .createNativeQuery(
          """
          select count(*) from (
              select distinct k.project_id, k.name, k.namespace_id
              from key k
              join project p on p.id = k.project_id
              left join branch b on b.id = k.branch_id
              where p.id = :projectId
                and k.deleted_at is null
                and (k.branch_id is null or b.deleted_at is null)
                and (p.use_branching = true or k.branch_id is null or b.is_default = true)
          ) sub
          """.trimIndent(),
        ).setParameter("projectId", projectId)
        .singleResult as Number
    ).toLong()
  }

  /**
   * Translation contribution of a single project, applying the same rules as
   * [getTranslationCount]. See [getProjectKeyContribution] for usage notes.
   */
  fun getProjectTranslationContribution(projectId: Long): Long {
    return entityManager
      .createNativeQuery(
        """
        with project_keys as materialized (
          select k.id, k.project_id, k.name, k.namespace_id
          from key k
          join project p on p.id = k.project_id
          left join branch b on b.id = k.branch_id
          where p.id = :projectId
            and k.deleted_at is null
            and (k.branch_id is null or b.deleted_at is null)
            and (p.use_branching = true or k.branch_id is null or b.is_default = true)
        )
        select count(*) from (
          select distinct pk.project_id, pk.name, pk.namespace_id, t.language_id
          from project_keys pk
          join translation t on t.key_id = pk.id and t.text is not null and t.text <> ''
          join language l on l.id = t.language_id and l.deleted_at is null
        ) sub
        """.trimIndent(),
      ).setParameter("projectId", projectId)
      .singleResult
      .let { (it as Number).toLong() }
  }

  /**
   * Translation contribution of a single language (across all projects it belongs to in
   * the same organization). Used when a language is being soft-deleted on its own.
   *
   * Returns the count that would be lost if the language were soft-deleted.
   */
  fun getLanguageTranslationContribution(languageId: Long): Long {
    return entityManager
      .createNativeQuery(
        """
        select count(*) from (
          select distinct k.project_id, k.name, k.namespace_id, t.language_id
          from translation t
          join key k on k.id = t.key_id and k.deleted_at is null
          join project p on p.id = k.project_id and p.deleted_at is null
          left join branch b on b.id = k.branch_id
          where t.language_id = :languageId
            and t.text is not null
            and t.text <> ''
            and (k.branch_id is null or b.deleted_at is null)
            and (p.use_branching = true or k.branch_id is null or b.is_default = true)
        ) sub
        """.trimIndent(),
      ).setParameter("languageId", languageId)
      .singleResult
      .let { (it as Number).toLong() }
  }
}
