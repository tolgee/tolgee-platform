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
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.webhook.WebhookConfig
import io.tolgee.repository.AutomationRepository
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class AutomationService(
  private val cacheManager: CacheManager,
  private val entityManager: EntityManager,
  private val automationRepository: AutomationRepository,
  @Suppress("SelfReferenceConstructorParameter") @Lazy
  private val self: AutomationService,
) {
  @Transactional
  fun getProjectAutomations(
    projectId: Long,
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType? = null,
  ): List<AutomationDto> {
    val forNullActivityType = self.getProjectAutomationsCaching(projectId, automationTriggerType, null)
    if (activityType == null) {
      return forNullActivityType
    }
    return self.getProjectAutomationsCaching(projectId, automationTriggerType, activityType) + forNullActivityType
  }

  @Cacheable(value = [Caches.AUTOMATIONS], key = "{#projectId, #automationTriggerType, #activityType}")
  @Transactional
  fun getProjectAutomationsCaching(
    projectId: Long,
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType? = null,
  ): List<AutomationDto> {
    val automations = getAutomationWithFetchedData(projectId, automationTriggerType, activityType)
    return automations.map { AutomationDto.fromEntity(it) }
  }

  @Transactional
  fun save(automation: Automation): Automation {
    automation.triggers.forEach { entityManager.persist(it) }
    automation.actions.forEach { entityManager.persist(it) }
    entityManager.persist(automation)

    if (automation.id > 0) {
      automation.triggers.forEach {
        getCache().evict(arrayListOf(automation.project.id, it.type, it.activityType))
      }
    }
    return automation
  }

  @Transactional
  fun delete(automation: Automation) {
    automation.triggers.forEach {
      getCache().evict(arrayListOf(automation.project.id, it.type, it.activityType))
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
    return entityManager
      .createQuery(
        """
      from AutomationAction aa 
      join fetch aa.automation
      where aa.id = :actionId
      """,
        AutomationAction::class.java,
      ).setParameter("actionId", actionId)
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
  fun createForContentDelivery(contentDeliveryConfig: ContentDeliveryConfig): Automation {
    val automation = Automation(entityManager.getReference(Project::class.java, contentDeliveryConfig.project.id))
    addContentDeliveryTriggersAndActions(contentDeliveryConfig, automation)
    contentDeliveryConfig.automationActions.addAll(automation.actions)
    return save(automation)
  }

  @Transactional
  fun createForSlackIntegration(slackConfig: SlackConfig): Automation {
    val automation = Automation(slackConfig.project)
    addSlackSubscriptionTriggersAndActions(slackConfig, automation)
    slackConfig.automationActions.addAll(automation.actions)
    return save(automation)
  }

  @Transactional
  fun updateForSlackConfig(slackConfig: SlackConfig): Automation {
    val automation = getAutomationForExistingSlackConfig(slackConfig)
    updateSlackTriggersAndActions(slackConfig, automation)
    slackConfig.automationActions.clear()
    slackConfig.automationActions.addAll(automation.actions)
    return save(automation)
  }

  private fun getAutomationForExistingSlackConfig(slackConfig: SlackConfig): Automation {
    val automations = slackConfig.automationActions.map { it.automation }
    if (automations.size == 1) {
      return automations[0]
    }
    automations.forEach {
      delete(it)
    }
    return createForSlackIntegration(slackConfig)
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

  private fun getAutomationForExistingWebhookConfig(webhookConfig: WebhookConfig): Automation {
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
      },
    )

    automation.actions.add(
      AutomationAction(automation).apply {
        this.type = AutomationActionType.WEBHOOK
        this.webhookConfig = webhookConfig
      },
    )
  }

  private fun addSlackSubscriptionTriggersAndActions(
    slackConfig: SlackConfig,
    automation: Automation,
  ) {
    automation.triggers.add(
      AutomationTrigger(automation).apply {
        this.type = AutomationTriggerType.ACTIVITY
        this.activityType = null
        this.debounceDurationInMs = 0
      },
    )

    automation.actions.add(
      AutomationAction(automation).apply {
        this.type = AutomationActionType.SLACK_SUBSCRIPTION
        this.slackConfig = slackConfig
      },
    )
  }

  private fun updateWebhookTriggersAndActions(
    webhookConfig: WebhookConfig,
    automation: Automation,
  ) {
    deleteTriggersAndActions(automation)
    addWebhookTriggersAndActions(webhookConfig, automation)
  }

  private fun updateSlackTriggersAndActions(
    slackConfig: SlackConfig,
    automation: Automation,
  ) {
    deleteTriggersAndActions(automation)
    addSlackSubscriptionTriggersAndActions(slackConfig, automation)
  }

  @Transactional
  fun removeForContentDelivery(contentDeliveryConfig: ContentDeliveryConfig) {
    contentDeliveryConfig.automationActions.forEach {
      delete(it.automation)
    }
    contentDeliveryConfig.automationActions.clear()
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
  fun delete(
    projectId: Long,
    automationId: Long,
  ) {
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
  fun deleteForSlackIntegration(slackConfig: SlackConfig) {
    slackConfig.automationActions.forEach {
      delete(it.automation)
    }
    slackConfig.automationActions.clear()
  }

  @Transactional
  fun get(
    projectId: Long,
    automationId: Long,
  ): Automation {
    return automationRepository.findByIdAndProjectId(automationId, projectId)
      ?: throw NotFoundException()
  }

  private fun getAutomationWithFetchedData(
    projectId: Long,
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType?,
  ): MutableList<Automation> {
    val automations = getAutomationsWithTriggerOfType(projectId, automationTriggerType, activityType)

    return entityManager
      .createQuery(
        """from Automation a join fetch a.actions where a in :automations""",
        Automation::class.java,
      ).setParameter("automations", automations)
      .resultList
  }

  private fun getAutomationsWithTriggerOfType(
    projectId: Long,
    automationTriggerType: AutomationTriggerType,
    activityType: ActivityType?,
  ): MutableList<Automation>? =
    entityManager
      .createQuery(
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
        Automation::class.java,
      ).setParameter("projectId", projectId)
      .setParameter("automationTriggerType", automationTriggerType)
      .setParameter("activityType", activityType)
      .resultList

  private fun addContentDeliveryTriggersAndActions(
    contentDeliveryConfig: ContentDeliveryConfig,
    automation: Automation,
  ) {
    automation.triggers.add(
      AutomationTrigger(automation).apply {
        this.type = AutomationTriggerType.TRANSLATION_DATA_MODIFICATION
        this.debounceDurationInMs = 30000
      },
    )

    automation.actions.add(
      AutomationAction(automation).apply {
        this.type = AutomationActionType.CONTENT_DELIVERY_PUBLISH
        this.contentDeliveryConfig = contentDeliveryConfig
      },
    )
  }
}
