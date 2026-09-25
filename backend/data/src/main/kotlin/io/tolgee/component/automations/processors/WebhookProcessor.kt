package io.tolgee.component.automations.processors

import io.tolgee.activity.ActivityService
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.batch.ChunkItemFailedException
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.component.automations.AutomationTriggerContext
import io.tolgee.constants.Message
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.webhook.WebhookConfig
import io.tolgee.security.ProjectHolder
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component

@Component
class WebhookProcessor(
  val projectHolder: ProjectHolder,
  val activityModelAssembler: IProjectActivityModelAssembler,
  val activityService: ActivityService,
  val currentDateProvider: CurrentDateProvider,
  val webhookExecutor: WebhookExecutor,
  val entityManager: EntityManager,
  val webhookAutoDisableChecker: WebhookAutoDisableChecker,
) : AutomationProcessor {
  override fun process(
    action: AutomationAction,
    context: AutomationTriggerContext,
  ) {
    val config = action.webhookConfig ?: return
    if (!config.enabled) return
    val data = buildRequest(config, context) ?: return

    try {
      webhookExecutor.signAndExecute(config, data)
      updateEntity(webhookConfig = config, failing = false)
    } catch (e: Exception) {
      updateEntity(config, true)
      if (webhookAutoDisableChecker.checkAfterFailure(config)) return
      when (e) {
        is WebhookRespondedWithNon200Status -> throw ChunkItemFailedException(
          Message.WEBHOOK_RESPONDED_WITH_NON_200_STATUS,
          cause = e,
          delayInMs = 5000,
        )

        else -> throw ChunkItemFailedException(
          Message.UNEXPECTED_ERROR_WHILE_EXECUTING_WEBHOOK,
          cause = e,
          delayInMs = 5000,
        )
      }
    }
  }

  private fun buildRequest(
    config: WebhookConfig,
    context: AutomationTriggerContext,
  ): WebhookRequest? {
    if (context.contentDeliveryPublish != null) {
      return WebhookRequest(
        webhookConfigId = config.id,
        projectId = config.project.id,
        eventType = WebhookEventType.CONTENT_DELIVERY_PUBLISH,
        activityData = null,
        contentDeliveryConfig = context.contentDeliveryPublish,
      )
    }
    val activityRevisionId = context.activityRevisionId ?: return null
    val view = activityService.findProjectActivity(activityRevisionId) ?: return null
    return WebhookRequest(
      webhookConfigId = config.id,
      projectId = config.project.id,
      eventType = WebhookEventType.PROJECT_ACTIVITY,
      activityData = activityModelAssembler.toModel(view),
    )
  }

  fun updateEntity(
    webhookConfig: WebhookConfig,
    failing: Boolean,
  ) {
    webhookConfig.lastExecuted = currentDateProvider.date
    if (!failing) {
      webhookConfig.firstFailed = null
      webhookConfig.autoDisableNotified = false
      entityManager.persist(webhookConfig)
      return
    }
    if (webhookConfig.firstFailed == null) {
      webhookConfig.firstFailed = currentDateProvider.date
    }
    entityManager.persist(webhookConfig)
  }
}
