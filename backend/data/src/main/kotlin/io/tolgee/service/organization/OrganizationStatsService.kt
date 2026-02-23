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
        where k.project.id = :projectId and k.project.deletedAt is null
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
        select count(*) from (
          select distinct k.project_id, k.name, k.namespace_id, t.language_id
          from translation t
          join key k on k.id = t.key_id
          where k.project_id in (
            select p.id from project p
            where p.organization_owner_id = :organizationId
              and p.deleted_at is null
          )
          and exists (
            select 1 from language l
            where l.id = t.language_id
              and l.deleted_at is null
          )
          and t.text is not null
          and t.text <> ''
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
              where p.organization_owner_id = :organizationId
          ) sub
          """.trimIndent(),
        ).setParameter("organizationId", organizationId)
        .singleResult as Number
    ).toLong()
  }
}
