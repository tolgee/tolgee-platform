package io.tolgee.component.automations.processors

import io.tolgee.activity.ActivityService
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.model.automations.AutomationAction

class SlackSubscriptionProcessor(
  private val activityService: ActivityService
): AutomationProcessor {
  override fun process(action: AutomationAction, activityRevisionId: Long?) {
    if(activityRevisionId == null) return

    val view = activityService.getProjectActivity(activityRevisionId)
  }
}
