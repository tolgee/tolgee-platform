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
      .createQuery(
        """
        select count(distinct k.project.id, k.name, k.namespace, t.language) from Translation t
        join t.key k
        join k.project p on p.deletedAt is null
        join t.language l on l.deletedAt is null
        where p.organizationOwner.id = :organizationId and t.text is not null and t.text <> ''
        """.trimIndent(),
      ).setParameter("organizationId", organizationId)
      .singleResult as Long
  }

  fun getKeyCount(organizationId: Long): Long {
    return entityManager
      .createQuery(
        """
        select count(distinct k.project.id, k.name, k.namespace) from Key k
        join k.project p on p.deletedAt is null
        where p.organizationOwner.id = :organizationId
        """.trimIndent(),
      ).setParameter("organizationId", organizationId)
      .singleResult as Long
  }
}
