package io.tolgee.service.automations

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Caches
import io.tolgee.dtos.cacheable.automations.AutomationDto
import io.tolgee.model.automations.Automation
import io.tolgee.model.automations.AutomationTriggerType
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class AutomationService(
  val cacheManager: CacheManager,
  val entityManager: EntityManager
) {
  @Cacheable(value = [Caches.AUTOMATIONS], key = "{#projectId, #automationTriggerType, #activityType}")
  fun getProjectAutomations(
    projectId: Long,
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType? = null
  ): List<AutomationDto> {
    val automations = getAutomationWithFetchedData(projectId, automationTriggerType, activityType)
    return automations.map { AutomationDto.fromEntity(it) }
  }

  private fun getAutomationWithFetchedData(
    projectId: Long,
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType? = null
  ): MutableList<Automation> {
    val automations = getAutomationsWithTriggerOfType(projectId, automationTriggerType, activityType)

    return entityManager.createQuery(
      "from Automation a join fetch a.actions where a in :automations",
      Automation::class.java
    ).setParameter("automations", automations).resultList
  }

  private fun getAutomationsWithTriggerOfType(
    projectId: Long,
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType?
  ): MutableList<Automation>? = entityManager.createQuery(
    """
            from Automation a join fetch a.triggers 
            where a.id in (
                select a2.id from Automation a2 
                join a2.triggers at 
                  where a2.project.id = :projectId
                   and at.type = :automationTriggerType
                   and (at.activityType = :activityType or (:activityType is null and at.activityType is null))
            )
    """.trimIndent(),
    Automation::class.java
  )
    .setParameter("projectId", projectId)
    .setParameter("automationTriggerType", automationTriggerType)
    .setParameter("activityType", activityType)
    .resultList

  @Transactional
  fun save(automation: Automation) {
    automation.triggers.forEach { entityManager.persist(it) }
    automation.actions.forEach { entityManager.persist(it) }
    entityManager.persist(automation)

    automation.triggers.forEach {
      getCache().evict(listOf(automation.project.id, it.type, it.activityType))
    }
  }

  @Transactional
  fun delete(automation: Automation) {
    automation.triggers.forEach {
      getCache().evict(listOf(automation.project.id, it.type, it.activityType))
    }
  }

  private fun getCache(): Cache {
    return cacheManager.getCache(Caches.AUTOMATIONS)!!
  }
}
