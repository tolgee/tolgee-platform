package io.tolgee.component.automations.processors.slackIntegration

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.projectActivityView.ProjectActivityViewByRevisionProvider
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.slackIntegration.EventName
import io.tolgee.model.slackIntegration.SlackConfig
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class SlackSubscriptionProcessor(
  private val activityModelAssembler: IProjectActivityModelAssembler,
  private val slackExecutor: SlackExecutor,
  private val applicationContext: ApplicationContext,
) : AutomationProcessor {
  override fun process(
    action: AutomationAction,
    activityRevisionId: Long?,
  ) {
    if (activityRevisionId == null) return

    val view =
      ProjectActivityViewByRevisionProvider(
        applicationContext = applicationContext,
        activityRevisionId,
        onlyCountInListAbove = 10,
      ).get() ?: return

    val activityModel = activityModelAssembler.toModel(view)

    val data = SlackRequest(activityData = activityModel)
    val config = action.slackConfig ?: return

    when (activityModel.type) {
      ActivityType.CREATE_KEY -> slackExecutor.sendMessageOnKeyAdded(config, data)
      ActivityType.SET_TRANSLATIONS, ActivityType.SET_TRANSLATION_STATE ->
        slackExecutor.sendMessageOnTranslationSet(config, data)
      ActivityType.IMPORT -> slackExecutor.sendMessageOnImport(config, data)
      else -> { }
    }
  }

  private fun checkSavedEvent(
    config: SlackConfig,
    activity: ActivityType,
  ): Boolean {
    if (config.onEvent == EventName.ALL) return true
    return when (activity) {
      ActivityType.CREATE_KEY -> config.onEvent == EventName.NEW_KEY
      ActivityType.SET_TRANSLATIONS, ActivityType.SET_TRANSLATION_STATE ->
        config.onEvent == EventName.TRANSLATION_CHANGED || config.onEvent == EventName.BASE_CHANGED
      else -> false
    }
  }
}
