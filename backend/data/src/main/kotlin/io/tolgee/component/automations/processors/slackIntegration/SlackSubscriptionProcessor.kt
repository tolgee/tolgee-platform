package io.tolgee.component.automations.processors.slackIntegration

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.ActivityType
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.model.slackIntegration.SlackConfig
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

    val data = SlackRequest(activityData = activityModel)
    val config = action.slackConfig ?: return
    slackExecutor.setHelper(data = data, slackConfig = config)

    // Checks if the saved(by user) onEvent matches the current activity type.
    if(!checkSavedEvent(config, activityModel.type))
      return
    when (activityModel.type) {
      ActivityType.KEY_NAME_EDIT, ActivityType.COMPLEX_EDIT -> slackExecutor.sendMessageOnKeyChange()
      ActivityType.CREATE_KEY -> slackExecutor.sendMessageOnKeyAdded()
      ActivityType.SET_TRANSLATIONS -> slackExecutor.sendMessageOnTranslationSet()
      else -> {  }
    }
  }

  private fun checkSavedEvent(config: SlackConfig, activity: ActivityType): Boolean {
    if(config.onEvent == EventName.ALL) return true
    return when (activity) {
      ActivityType.CREATE_KEY ->  config.onEvent == EventName.NEW_KEY
      ActivityType.SET_TRANSLATIONS -> config.onEvent == EventName.TRANSLATION_CHANGED || config.onEvent == EventName.BASE_CHANGED
      else -> false
    }
  }

}
