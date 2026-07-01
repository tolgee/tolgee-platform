package io.tolgee.service.apps

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.Project
import io.tolgee.model.apps.AppInstall
import io.tolgee.model.automations.Automation
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationActionType
import io.tolgee.model.automations.AutomationTrigger
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.model.webhook.WebhookConfig
import io.tolgee.repository.WebhookConfigRepository
import io.tolgee.repository.apps.AppEnabledForProjectRepository
import io.tolgee.service.automations.AutomationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppManagedAutomationService(
  private val automationService: AutomationService,
  private val webhookConfigRepository: WebhookConfigRepository,
  private val appEnabledForProjectRepository: AppEnabledForProjectRepository,
) {
  @Transactional
  fun onEnable(
    install: AppInstall,
    project: Project,
  ) {
    if (install.webhookSubscriptions.isEmpty() || install.webhookUrl == null) {
      return
    }
    val webhookConfig =
      WebhookConfig(project = project).apply {
        this.url = install.webhookUrl!!
        this.webhookSecret = install.webhookSecret ?: ""
        this.appInstall = install
        this.enabled = true
      }
    webhookConfigRepository.save(webhookConfig)

    val automation = Automation(project)
    install.webhookSubscriptions.forEach { event ->
      automation.triggers.add(triggerFor(event, automation))
    }
    automation.actions.add(
      AutomationAction(automation).apply {
        this.type = AutomationActionType.WEBHOOK
        this.webhookConfig = webhookConfig
      },
    )
    webhookConfig.automationActions.addAll(automation.actions)
    automationService.save(automation)
  }

  @Transactional
  fun onDisable(
    install: AppInstall,
    project: Project,
  ) {
    val webhookConfig =
      webhookConfigRepository.findByAppInstallAndProjectId(install, project.id) ?: return
    automationService.deleteForWebhookConfig(webhookConfig)
    webhookConfigRepository.delete(webhookConfig)
  }

  @Transactional
  fun onInstallRefresh(install: AppInstall) {
    val configs = webhookConfigRepository.findAllByAppInstall(install)
    if (install.webhookSubscriptions.isEmpty() || install.webhookUrl == null) {
      configs.forEach { config ->
        automationService.deleteForWebhookConfig(config)
        webhookConfigRepository.delete(config)
      }
      return
    }
    configs.forEach { config ->
      config.url = install.webhookUrl!!
      webhookConfigRepository.save(config)
      val automation = automationForWebhookConfig(config) ?: return@forEach
      rebuildTriggers(automation, install.webhookSubscriptions)
      automationService.save(automation)
    }
    val configuredProjectIds = configs.map { it.project.id }.toSet()
    val enablements = appEnabledForProjectRepository.findAllByAppInstallId(install.id)
    enablements
      .filter { it.project.id !in configuredProjectIds }
      .forEach { onEnable(install, it.project) }
  }

  private fun automationForWebhookConfig(config: WebhookConfig): Automation? {
    val automations = config.automationActions.mapNotNull { it.automation }.distinct()
    return automations.singleOrNull()
  }

  private fun rebuildTriggers(
    automation: Automation,
    events: Set<String>,
  ) {
    automation.triggers.clear()
    events.forEach { automation.triggers.add(triggerFor(it, automation)) }
  }

  private fun triggerFor(
    event: String,
    automation: Automation,
  ): AutomationTrigger {
    val activityType =
      runCatching { ActivityType.valueOf(event) }.getOrNull()
        ?: error("Unknown app event '$event' — manifest validation should have rejected this earlier")
    return AutomationTrigger(automation).apply {
      this.type = AutomationTriggerType.ACTIVITY
      this.activityType = activityType
      this.debounceDurationInMs = 0
    }
  }
}
