package io.tolgee.component.automations.processors

import io.tolgee.activity.ActivityService
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.model.automations.AutomationAction
import io.tolgee.security.ProjectHolder
import org.springframework.stereotype.Component

@Component
class WebhookProcessor(
  val projectHolder: ProjectHolder,
  val activityModelAssembler: IProjectActivityModelAssembler,
  val activityService: ActivityService,
  val currentDateProvider: CurrentDateProvider,
  val webhookExecutor: WebhookExecutor
) : AutomationProcessor {
  override fun process(action: AutomationAction, activityRevisionId: Long?) {
    activityRevisionId ?: return
    val view = activityService.getProjectActivity(activityRevisionId) ?: return
    val activityModel = activityModelAssembler.toModel(view)
    val config = action.webhookConfig ?: return

    val data = WebhookRequest(
      webhookConfigId = config.id,
      eventType = WebhookEventType.PROJECT_ACTIVITY,
      activityData = activityModel
    )

    webhookExecutor.signAndExecute(config, data)
  }
}
