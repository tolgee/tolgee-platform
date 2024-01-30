package io.tolgee.component.automations.processors.slackIntegration

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.ActivityType
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.model.automations.AutomationAction
import org.springframework.stereotype.Component

@Component
class SlackSubscriptionProcessor(
  private val activityService: ActivityService,
  private val activityModelAssembler: IProjectActivityModelAssembler,
  private val slackExecutor: SlackExecutor,
): AutomationProcessor {
  override fun process(action: AutomationAction, activityRevisionId: Long?) {
    if(activityRevisionId == null) return

    val view = activityService.getProjectActivity(activityRevisionId) ?: return
    val activityModel = activityModelAssembler.toModel(view)

    val data = SlackRequest(
      activityData = activityModel
    )
    val config = action.slackConfig ?: return

    when(activityModel.type) {
      ActivityType.KEY_DELETE, ActivityType.KEY_NAME_EDIT, ActivityType.KEY_TAGS_EDIT -> {
        slackExecutor.sendMessageOnKeyChange(
          slackConfig = config,
          data = data
        )
      }

      ActivityType.CREATE_KEY -> {
        slackExecutor.sendMessageOnKeyAdded(
          slackConfig = config,
          data = data
        )
      }
      else -> {
        slackExecutor.sendMessageOnKeyChange(
          slackConfig = config,
          data = data
        )
      }
    }
  }
}
