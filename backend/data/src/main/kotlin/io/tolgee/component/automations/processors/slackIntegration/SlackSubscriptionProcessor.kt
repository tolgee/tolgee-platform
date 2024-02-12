package io.tolgee.component.automations.processors.slackIntegration

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.ActivityType
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.enums.Scope
import io.tolgee.service.security.PermissionService
import org.springframework.stereotype.Component

@Component
class SlackSubscriptionProcessor(
  private val activityService: ActivityService,
  private val activityModelAssembler: IProjectActivityModelAssembler,
  private val slackExecutor: SlackExecutor,
  private val permissionService: PermissionService,
): AutomationProcessor {
  override fun process(action: AutomationAction, activityRevisionId: Long?) {
    if(activityRevisionId == null) return

    val view = activityService.getProjectActivity(activityRevisionId) ?: return
    val activityModel = activityModelAssembler.toModel(view)

    val data = SlackRequest(activityData = activityModel)
    val config = action.slackConfig ?: return
    slackExecutor.setHelper(data = data, slackConfig = config)

    when(activityModel.type) {
      ActivityType.KEY_NAME_EDIT, ActivityType.COMPLEX_EDIT -> {
        if(permissionService.getProjectPermissionScopes(config.project.id, config.userAccount.id)
            ?.contains(Scope.TRANSLATIONS_EDIT) == true
        ) {
          slackExecutor.sendMessageOnKeyChange()
        }
      }

      ActivityType.CREATE_KEY -> {
        slackExecutor.sendMessageOnKeyAdded()
      }
      else -> {
      }
    }
  }
}
