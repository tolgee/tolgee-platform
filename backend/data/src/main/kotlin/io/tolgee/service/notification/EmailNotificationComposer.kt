package io.tolgee.service.notification

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.constants.NotificationType
import io.tolgee.model.Notification
import io.tolgee.model.enums.TaskType
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class EmailNotificationComposer(
  private val frontendUrlProvider: FrontendUrlProvider,
  private val i18n: I18n,
) {
  fun composeEmailSubject(notification: Notification) =
    i18n.translate("notifications.email.subject.${notification.type}")

  fun composeEmailText(notification: Notification) =
    when (notification.type) {
      NotificationType.TASK_ASSIGNED, NotificationType.TASK_COMPLETED, NotificationType.TASK_CLOSED ->
        """
          |${taskHeader(notification)}:
          |<br/>
          |${taskLink(notification)}
          |<br/><br/>
          |${checkAllYourTasksFooter()}
        """.trimMargin()

      NotificationType.MFA_ENABLED, NotificationType.MFA_DISABLED ->
        """
          |${multiFactorChangedMessage(notification)}
          |<br/><br/>
          |${checkYourSecuritySettingsFooter()}
        """.trimMargin()

      NotificationType.PASSWORD_CHANGED ->
        """
          |${passwordChangedMessage()}
          |<br/><br/>
          |${checkYourSecuritySettingsFooter()}
        """.trimMargin()
    }

  private fun taskHeader(notification: Notification) =
    i18n
      .translate("notifications.email.task-header.${notification.type}")

  private fun checkAllYourTasksFooter() =
    i18n
      .translate("notifications.email.my-tasks-link")
      .replace("\${myTasksUrl}", frontendUrlProvider.getMyTasksUrl())

  private fun checkYourSecuritySettingsFooter() =
    i18n
      .translate("notifications.email.security-settings-link")
      .replace("\${securitySettingsUrl}", frontendUrlProvider.getAccountSecurityUrl())

  private fun taskLink(notification: Notification): String {
    val task =
      notification.linkedTask
        ?: throw IllegalStateException(
          "Notification of type ${notification.type} must contain linkedTask.",
        )
    return """
        |<a href="${frontendUrlProvider.getTaskUrl(task.project.id, task.number)}">
        |  ${task.name} #${task.number} (${task.language.name}) - ${taskType(task.type)}
        |</a>
      """.trimMargin()
  }

  private fun taskType(type: TaskType): String = i18n.translate("notifications.email.task-type.$type")

  private fun multiFactorChangedMessage(notification: Notification): String {
    val enabledDisabled = i18n.translate("notifications.email.mfa.${notification.type}")
    return i18n
      .translate("notifications.email.mfa.changed")
      .replace("\${enabledDisabled}", enabledDisabled)
  }

  private fun passwordChangedMessage() = i18n.translate("notifications.email.password-changed")
}
