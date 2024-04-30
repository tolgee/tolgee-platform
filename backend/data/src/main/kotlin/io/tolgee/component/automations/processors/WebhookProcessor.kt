package io.tolgee.component.automations.processors

import io.tolgee.activity.ActivityService
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.batch.RequeueWithDelayException
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.automations.AutomationProcessor
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
) : AutomationProcessor {
  override fun process(
    action: AutomationAction,
    activityRevisionId: Long?,
  ) {
    activityRevisionId ?: return
    val view = activityService.findProjectActivity(activityRevisionId) ?: return
    val activityModel = activityModelAssembler.toModel(view)
    val config = action.webhookConfig ?: return

    val data =
      WebhookRequest(
        webhookConfigId = config.id,
        eventType = WebhookEventType.PROJECT_ACTIVITY,
        activityData = activityModel,
      )

    try {
      webhookExecutor.signAndExecute(config, data)
      updateEntity(webhookConfig = config, failing = false)
    } catch (e: Exception) {
      updateEntity(config, true)
      when (e) {
        is WebhookRespondedWithNon200Status -> throw RequeueWithDelayException(
          Message.WEBHOOK_RESPONDED_WITH_NON_200_STATUS,
          cause = e,
          delayInMs = 5000,
        )

        else -> throw RequeueWithDelayException(
          Message.UNEXPECTED_ERROR_WHILE_EXECUTING_WEBHOOK,
          cause = e,
          delayInMs = 5000,
        )
      }
    }
  }

  fun updateEntity(
    webhookConfig: WebhookConfig,
    failing: Boolean,
  ) {
    webhookConfig.firstFailed = if (failing) currentDateProvider.date else null
    webhookConfig.lastExecuted = currentDateProvider.date
    entityManager.persist(webhookConfig)
  }
}
