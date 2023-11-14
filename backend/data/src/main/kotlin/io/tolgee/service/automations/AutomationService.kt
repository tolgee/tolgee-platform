package io.tolgee.service.automations

import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Caches
import io.tolgee.dtos.cacheable.automations.AutomationDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.automations.Automation
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationActionType
import io.tolgee.model.automations.AutomationTrigger
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.model.cdn.Cdn
import io.tolgee.model.webhook.WebhookConfig
import io.tolgee.repository.AutomationRepository
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class AutomationService(
  private val cacheManager: CacheManager,
  private val entityManager: EntityManager,
  private val automationRepository: AutomationRepository,
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
    automationRepository.delete(automation)
  }

  @Transactional
  fun delete(id: Long) {
    val automation = get(id)
    delete(automation)
  }

  private fun getCache(): Cache {
    return cacheManager.getCache(Caches.AUTOMATIONS)!!
  }

  @Transactional
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

  private fun deleteTriggersAndActions(automation: Automation) {
    automation.actions.removeAll {
      entityManager.remove(it)
      true
    }
    automation.triggers.removeAll {
      entityManager.remove(it)
      true
    }
    automation.actions.clear()
    automation.triggers.clear()
  }

  @Transactional
  fun createForCdn(cdn: Cdn): Automation {
    val automation = Automation(entityManager.getReference(Project::class.java, cdn.project.id))
    addCdnTriggersAndActions(cdn, automation)
    cdn.automationActions.addAll(automation.actions)
    return save(automation)
  }

  @Transactional
  fun createForWebhookConfig(webhookConfig: WebhookConfig): Automation {
    val automation = Automation(webhookConfig.project)
    addWebhookTriggersAndActions(webhookConfig, automation)
    webhookConfig.automationActions.addAll(automation.actions)
    return save(automation)
  }

  @Transactional
  fun updateForWebhookConfig(webhookConfig: WebhookConfig): Automation {
    val automation = getAutomationForExistingWebhookConfig(webhookConfig)
    updateWebhookTriggersAndActions(webhookConfig, automation)
    webhookConfig.automationActions.clear()
    webhookConfig.automationActions.addAll(automation.actions)
    return save(automation)
  }

  private fun getAutomationForExistingWebhookConfig(
    webhookConfig: WebhookConfig,
  ): Automation {
    val automations = webhookConfig.automationActions.map { it.automation }
    if (automations.size == 1) {
      return automations[0]
    }
    automations.forEach {
      delete(it)
    }
    return createForWebhookConfig(webhookConfig)
  }

  private fun addWebhookTriggersAndActions(
    webhookConfig: WebhookConfig,
    automation: Automation,
  ) {
    automation.triggers.add(
      AutomationTrigger(automation).apply {
        this.type = AutomationTriggerType.ACTIVITY
        this.activityType = null
        this.debounceDurationInMs = 0
      }
    )

    automation.actions.add(
      AutomationAction(automation).apply {
        this.type = AutomationActionType.WEBHOOK
        this.webhookConfig = webhookConfig
      }
    )
  }

  private fun updateWebhookTriggersAndActions(
    webhookConfig: WebhookConfig,
    automation: Automation,
  ) {
    deleteTriggersAndActions(automation)
    addWebhookTriggersAndActions(webhookConfig, automation)
  }


  @Transactional
  fun removeForCdn(cdn: Cdn) {
    cdn.automationActions.forEach {
      delete(it.automation)
    }
    cdn.automationActions.clear()
  }

  @Transactional
  fun get(id: Long): Automation {
    return find(id) ?: throw NotFoundException()
  }

  @Transactional
  fun find(id: Long): Automation? {
    return automationRepository.find(id)
  }

  @Transactional
  fun delete(projectId: Long, automationId: Long) {
    val deletedCunt = automationRepository.deleteByIdAndProjectId(automationId, projectId)
    if (deletedCunt == 0L) {
      throw NotFoundException()
    }
  }

  fun deleteForWebhookConfig(webhookConfig: WebhookConfig) {
    webhookConfig.automationActions.forEach {
      delete(it.automation)
    }
    webhookConfig.automationActions.clear()
  }

  @Transactional
  fun get(projectId: Long, automationId: Long): Automation {
    return automationRepository.findByIdAndProjectId(automationId, projectId)
      ?: throw NotFoundException()
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
                   and (at.activityType = :activityType or at.activityType is null)
            )
    """.trimIndent(),
    Automation::class.java
  )
    .setParameter("projectId", projectId)
    .setParameter("automationTriggerType", automationTriggerType)
    .setParameter("activityType", activityType)
    .resultList

  private fun addCdnTriggersAndActions(
    cdn: Cdn,
    automation: Automation
  ) {
    automation.triggers.add(
      AutomationTrigger(automation).apply {
        this.type = AutomationTriggerType.TRANSLATION_DATA_MODIFICATION
        this.debounceDurationInMs = 5000
      }
    )

    automation.actions.add(
      AutomationAction(automation).apply {
        this.type = AutomationActionType.CDN_PUBLISH
        this.cdn = cdn
      }
    )
  }
}
