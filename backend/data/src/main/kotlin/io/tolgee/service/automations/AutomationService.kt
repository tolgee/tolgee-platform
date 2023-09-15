package io.tolgee.service.automations

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Caches
import io.tolgee.dtos.cacheable.automations.AutomationDto
import io.tolgee.dtos.request.automation.AutomationRequest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.automations.Automation
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationTrigger
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.repository.AutomationRepository
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class AutomationService(
  private val cacheManager: CacheManager,
  private val entityManager: EntityManager,
  private val applicationContext: ApplicationContext,
  private val automationRepository: AutomationRepository
) {
  @Cacheable(value = [Caches.AUTOMATIONS], key = "{#projectId, #automationTriggerType, #activityType}")
  @Transactional
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
      """from Automation a join fetch a.actions where a in :automations""",
      Automation::class.java
    ).setParameter("automations", automations).resultList
  }

  private fun getAutomationWithFetchedData(
    automationId: Long,
    projectId: Long
  ): Automation {
    return entityManager.createQuery(
      """from Automation a 
        |join fetch a.actions 
        |join fetch a.triggers 
        |where a.id = :automationId 
        |and a.project.id = :projectId""".trimMargin(),
      Automation::class.java
    )
      .setParameter("automationId", automationId)
      .setParameter("projectId", projectId)
      .singleResult
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
  fun save(automation: Automation): Automation {
    automation.triggers.forEach { entityManager.persist(it) }
    automation.actions.forEach { entityManager.persist(it) }
    entityManager.persist(automation)

    automation.triggers.forEach {
      getCache().evict(listOf(automation.project.id, it.type, it.activityType))
    }
    return automation
  }

  @Transactional
  fun delete(automation: Automation) {
    automation.triggers.forEach {
      getCache().evict(listOf(automation.project.id, it.type, it.activityType))
    }
  }

  @Transactional
  fun delete(id: Long) {
    automationRepository.deleteById(id)
  }

  private fun getCache(): Cache {
    return cacheManager.getCache(Caches.AUTOMATIONS)!!
  }

  fun getAction(actionId: Long): AutomationAction {
    return entityManager.createQuery(
      """
      from AutomationAction aa 
      join fetch aa.automation
      where aa.id = :actionId
      """,
      AutomationAction::class.java
    )
      .setParameter("actionId", actionId)
      .singleResult ?: throw NotFoundException()
  }

  @Transactional
  fun update(projectId: Long, automationId: Long, dto: AutomationRequest): Automation {
    val automation = getAutomationWithFetchedData(automationId, projectId)
    deleteTriggersAndActions(automation)
    setNewTriggersAndActions(dto, automation)
    return save(automation)
  }

  private fun deleteTriggersAndActions(automation: Automation) {
    automation.actions.removeAll {
      entityManager.remove(it)
      true
    }
    automation.triggers.removeAll {
      entityManager.remove(it)
      true
    }
  }

  @Transactional
  fun create(projectId: Long, dto: AutomationRequest): Automation {
    val automation = Automation(entityManager.getReference(Project::class.java, projectId))
    setNewTriggersAndActions(dto, automation)
    return save(automation)
  }

  private fun setNewTriggersAndActions(
    dto: AutomationRequest,
    automation: Automation
  ) {
    val triggers = dto.triggers.map {
      AutomationTrigger(automation).apply {
        this.activityType = it.activityType
        this.type = it.type
        this.debounceDurationInMs = it.debounceDurationInMs
      }
    }.toMutableList()

    val actions = dto.actions.map { actionRequest ->
      AutomationAction(automation).apply {
        this.type = actionRequest.type
        val processor = applicationContext.getBean(actionRequest.type.processor.java)
        this.params = processor.getParamsFromRequest(actionRequest)
      }
    }.toMutableList()

    automation.actions = actions
    automation.triggers = triggers
  }

  fun getProjectAutomations(projectId: Long, pageable: Pageable): Page<Automation> {
    val automations = automationRepository.findAllInProject(projectId, pageable)
    automationRepository.fetchTriggers(automations.content)
    return automations
  }

  fun get(id: Long): Automation {
    return find(id) ?: throw NotFoundException()
  }

  fun find(id: Long): Automation? {
    return automationRepository.find(id)
  }

  fun delete(projectId: Long, automationId: Long) {
    val deletedCunt = automationRepository.deleteByIdAndProjectId(automationId, projectId)
    if (deletedCunt == 0L) {
      throw NotFoundException()
    }
  }

  fun get(projectId: Long, automationId: Long): Automation {
    return automationRepository.findByIdAndProjectId(automationId, projectId)
      ?: throw NotFoundException()
  }
}
