package io.tolgee.ee.component.slackIntegration

import io.tolgee.activity.data.ActivityType
import io.tolgee.activity.projectActivity.ProjectActivityViewByRevisionProvider
import io.tolgee.api.IProjectActivityModelAssembler
import io.tolgee.component.automations.processors.SlackSubscriptionProcessor
import io.tolgee.ee.component.slackIntegration.data.SlackRequest
import io.tolgee.ee.component.slackIntegration.notification.SlackAutomationMessageSender
import io.tolgee.model.automations.AutomationAction
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class SlackSubscriptionProcessorImpl(
  private val activityModelAssembler: IProjectActivityModelAssembler,
  private val slackAutomationMessageSender: SlackAutomationMessageSender,
  private val applicationContext: ApplicationContext,
) : SlackSubscriptionProcessor {
  override fun process(
    action: AutomationAction,
    activityRevisionId: Long?,
  ) {
    if (activityRevisionId == null) return

    val view =
      ProjectActivityViewByRevisionProvider(
        applicationContext = applicationContext,
        activityRevisionId,
        onlyCountInListAbove = SlackAutomationMessageSender.MAX_NEW_MESSAGES_TO_SEND,
      ).get() ?: return

    val activityModel = activityModelAssembler.toModel(view)

    val data = SlackRequest(activityData = activityModel)
    val config = action.slackConfig ?: return

    when (activityModel.type) {
      ActivityType.CREATE_KEY -> slackAutomationMessageSender.sendMessageOnKeyAdded(config, data)
      in translationActivities -> slackAutomationMessageSender.sendMessageOnTranslationSet(config, data)
      ActivityType.IMPORT -> slackAutomationMessageSender.sendMessageOnImport(config, data)
      else -> {}
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
        ActivityType.AUTO_TRANSLATE,
      )
  }
}
