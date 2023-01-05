package io.tolgee.service

import org.hibernate.FlushMode
import org.hibernate.Session
import org.springframework.stereotype.Service
import java.math.BigDecimal
import javax.persistence.EntityManager
import javax.persistence.FlushModeType

@Service
class OrganizationStatsService(
  private val entityManager: EntityManager,
) {
  fun getProjectLanguageCount(projectId: Long): Long {
    return entityManager
      .createQuery(
        """
          select count(l) from Language l 
          where l.project.id = :projectId
        """.trimIndent()
      )
      .setParameter("projectId", projectId)
      .singleResult as Long
  }

  fun getProjectKeyCount(projectId: Long): Long {
    return entityManager
      .createQuery(
        """
          select count(k) from Key k
          where k.project.id = :projectId
        """.trimIndent()
      )
      .setParameter("projectId", projectId)
      .singleResult as Long
  }

  fun getCurrentTranslationCount(organizationId: Long): Long {
    val session = entityManager.unwrap(Session::class.java)
    return try {
      session.hibernateFlushMode = FlushMode.MANUAL
      session.flushMode = FlushModeType.COMMIT
      val query = session.createNativeQuery(
        """
      select
         (select sum(keyCount * languageCount) as translationCount
          from (select p.id as projectId, count(l.id) as languageCount
                from project as p
                         join language as l on l.project_id = p.id
                where p.organization_owner_id = :organizationId
                group by p.id) as languageCounts
                   join (select p.id as projectId, count(k.id) as keyCount
                         from project as p
                                  join key as k on k.project_id = p.id
                         where p.organization_owner_id = :organizationId
                         group by p.id) as keyCounts on keyCounts.projectId = languageCounts.projectId)
        """.trimIndent()
      ).setParameter("organizationId", organizationId)
      val result = query.singleResult as BigDecimal? ?: 0
      result.toLong()
    } finally {
      session.hibernateFlushMode = FlushMode.AUTO
      session.flushMode = FlushModeType.AUTO
    }
  }
}
