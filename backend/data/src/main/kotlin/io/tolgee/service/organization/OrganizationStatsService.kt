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

  fun getWordCount(organizationId: Long): Long {
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
          select ok.project_id, ok.name, ok.namespace_id, t.language_id,
                 coalesce(t.word_count, 0) as word_count
          from org_keys ok
          join translation t on t.key_id = ok.id
            and t.text is not null
            and t.text <> ''
        )
        select coalesce(sum(max_wc), 0) from (
          select max(ot.word_count) as max_wc
          from org_translations ot
          where exists (
            select 1 from language l
            where l.id = ot.language_id
              and l.deleted_at is null
          )
          group by ot.project_id, ot.name, ot.namespace_id, ot.language_id
        ) sub
        """.trimIndent(),
      ).setParameter("organizationId", organizationId)
      .singleResult
      .let { (it as Number).toLong() }
  }

  fun countAllWordsOnInstance(): Long {
    return entityManager
      .createNativeQuery(
        """
        with instance_keys as materialized (
          select k.id, k.project_id, k.name, k.namespace_id
          from key k
          join project p on p.id = k.project_id and p.deleted_at is null
          join organization o on o.id = p.organization_owner_id and o.deleted_at is null
          left join branch b on b.id = k.branch_id
          where k.deleted_at is null
            and (k.branch_id is null or b.deleted_at is null)
            and (p.use_branching = true or k.branch_id is null or b.is_default = true)
        ),
        instance_translations as materialized (
          select ik.project_id, ik.name, ik.namespace_id, t.language_id,
                 coalesce(t.word_count, 0) as word_count
          from instance_keys ik
          join translation t on t.key_id = ik.id
            and t.text is not null
            and t.text <> ''
        )
        select coalesce(sum(max_wc), 0) from (
          select max(it.word_count) as max_wc
          from instance_translations it
          where exists (
            select 1 from language l
            where l.id = it.language_id
              and l.deleted_at is null
          )
          group by it.project_id, it.name, it.namespace_id, it.language_id
        ) sub
        """.trimIndent(),
      ).singleResult
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
}
