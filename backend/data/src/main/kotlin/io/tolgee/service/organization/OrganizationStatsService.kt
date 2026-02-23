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
    return (
      entityManager
        .createNativeQuery(
          """
          select count(*) from (
              select distinct k.name, k.namespace_id
              from key k
              join project p on p.id = k.project_id and p.deleted_at is null
              where k.project_id = :projectId
          ) sub
          """.trimIndent(),
        ).setParameter("projectId", projectId)
        .singleResult as Number
    ).toLong()
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
    // Uses native SQL for two reasons:
    // 1. GROUP BY logical key (project_id, name, namespace_id) + SUM(COUNT DISTINCT language_id)
    //    is more optimizer-friendly than DISTINCT on 4 columns, because count(distinct)
    //    within small groups (one per branch per logical key) is cheap.
    // 2. Native SQL uses k.namespace_id directly, avoiding a potential unnecessary join
    //    to the namespace table that JPQL entity references can cause.
    return (
      entityManager
        .createNativeQuery(
          """
          select coalesce(sum(per_key_count), 0)
          from (
              select count(distinct t.language_id) as per_key_count
              from translation t
              join key k on k.id = t.key_id
              join project p on p.id = k.project_id and p.deleted_at is null
              join language l on l.id = t.language_id and l.deleted_at is null
              where p.organization_owner_id = :organizationId
              and t.text is not null and t.text <> ''
              group by k.project_id, k.name, k.namespace_id
          ) sub
          """.trimIndent(),
        ).setParameter("organizationId", organizationId)
        .singleResult as Number
    ).toLong()
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
              where p.organization_owner_id = :organizationId
          ) sub
          """.trimIndent(),
        ).setParameter("organizationId", organizationId)
        .singleResult as Number
    ).toLong()
  }
}
