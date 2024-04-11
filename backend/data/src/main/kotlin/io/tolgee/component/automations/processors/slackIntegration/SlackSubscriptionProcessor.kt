package io.tolgee.component.automations.processors.slackIntegration

import io.tolgee.activity.ActivityService
import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.projectActivityView.ProjectActivityViewByRevisionProvider
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.model.automations.AutomationAction
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class SlackSubscriptionProcessor(
  private val activityService: ActivityService,
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
    slackExecutor.setHelper(data = data, slackConfig = config)
    when (activityModel.type) {
      ActivityType.CREATE_KEY -> slackExecutor.sendMessageOnKeyAdded()
      in translationActivities -> slackExecutor.sendMessageOnTranslationSet()
      ActivityType.IMPORT -> slackExecutor.sendMessageOnImport()
      else -> { }
    }
  }

  companion object {
    val translationActivities =
      setOf(
        ActivityType.SET_TRANSLATIONS,
        ActivityType.SET_TRANSLATION_STATE,
        ActivityType.BATCH_SET_TRANSLATION_STATE,
        ActivityType.BATCH_MACHINE_TRANSLATE,
        ActivityType.BATCH_COPY_TRANSLATIONS,
        ActivityType.BATCH_CLEAR_TRANSLATIONS,
      )
  }
}
