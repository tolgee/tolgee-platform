package io.tolgee.service.organization

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import java.math.BigDecimal

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
      )
      .setParameter("projectId", projectId)
      .singleResult as Long
  }

  fun getProjectKeyCount(projectId: Long): Long {
    return entityManager
      .createQuery(
        """
        select count(k) from Key k
        where k.project.id = :projectId and k.project.deletedAt is null
        """.trimIndent(),
      )
      .setParameter("projectId", projectId)
      .singleResult as Long
  }

  fun getTranslationSlotCount(organizationId: Long): Long {
    val result =
      entityManager.createNativeQuery(
        """
        select 
           (select sum(keyCount * languageCount) as translationCount
            from (select p.id as projectId, count(l.id) as languageCount
                  from project as p
                           join language as l on l.project_id = p.id
                  where p.organization_owner_id = :organizationId and p.deleted_at is null
                  group by p.id) as languageCounts
                     join (select p.id as projectId, count(k.id) as keyCount
                           from project as p
                                    join key as k on k.project_id = p.id
                           where p.organization_owner_id = :organizationId and p.deleted_at is null
                           group by p.id) as keyCounts on keyCounts.projectId = languageCounts.projectId)
        """.trimIndent(),
      ).setParameter("organizationId", organizationId).singleResult as BigDecimal? ?: 0
    return result.toLong()
  }

  fun getSeatCount(organizationId: Long): Long {
    return entityManager.createQuery(
      """
      select count(distinct ua.id) from UserAccount ua
      left join ua.organizationRoles orl
      left join orl.organization o on o.deletedAt is null and o.id = :organizationId
      left join ua.permissions p 
      left join p.project pr on pr.deletedAt is null and pr.organizationOwner.id = :organizationId 
      where ua.deletedAt is null and ua.disabledAt is null
        and (pr is not null or o is not null)
      """.trimIndent(),
    ).setParameter("organizationId", organizationId).singleResult as Long
  }

  fun getTranslationCount(organizationId: Long): Long {
    return entityManager.createQuery(
      """
        select count(t.id) from Translation t
        join t.key k
        join k.project p on p.deletedAt is null
        join t.language l on l.deletedAt is null
        where p.organizationOwner.id = :organizationId and t.text is not null and t.text <> ''
        """.trimIndent(),
    ).setParameter("organizationId", organizationId).singleResult as Long
  }

  fun getKeyCount(organizationId: Long): Long {
    return entityManager.createQuery(
      """
        select count(k.id) from Key k
        join k.project p on p.deletedAt is null
        where p.organizationOwner.id = :organizationId
        """.trimIndent(),
    ).setParameter("organizationId", organizationId).singleResult as Long
  }

}
