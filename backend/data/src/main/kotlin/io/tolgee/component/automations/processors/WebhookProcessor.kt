package io.tolgee.component.automations.processors

import io.tolgee.activity.ActivityService
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.model.automations.AutomationAction
import org.springframework.stereotype.Component

@Component
class WebhookProcessor(
  private val activityModelAssembler: IProjectActivityModelAssembler,
  private val activityService: ActivityService,
  private val webhookDeliveryManager: WebhookDeliveryManager,
) : AutomationProcessor {
  override fun process(
    action: AutomationAction,
    activityRevisionId: Long?,
  ) {
    activityRevisionId ?: return
    val config = action.webhookConfig ?: return
    if (!config.eventTypes.contains(WebhookEventType.PROJECT_ACTIVITY)) return

    val view = activityService.findProjectActivity(activityRevisionId) ?: return
    val activityModel = activityModelAssembler.toModel(view)

    val data =
      WebhookRequest(
        webhookConfigId = config.id,
        projectId = config.project.id,
        eventType = WebhookEventType.PROJECT_ACTIVITY,
        activityData = activityModel,
      )

    webhookDeliveryManager.signExecuteAndHandle(config, data)
  }
}
