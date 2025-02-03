package io.tolgee.service.notification

import io.sentry.Sentry
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.component.email.TolgeeEmailSender
import io.tolgee.constants.NotificationType
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.model.Notification
import io.tolgee.model.enums.TaskType
import io.tolgee.model.task.Task
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component

@Component
class EmailNotificationsService(
  private val tolgeeEmailSender: TolgeeEmailSender,
  private val frontendUrlProvider: FrontendUrlProvider,
) : Logging {
  fun sendEmailNotification(notification: Notification) {
    val subject = composeEmailSubject(notification)
    val text = composeEmailText(notification)
    val params = EmailParams(
      to = notification.user.username,
      subject = subject,
      text =
        """
          |Hello!👋
          |<br/><br/>
          |$text
          |<br/><br/>
          |Regards,<br/>
          |Tolgee
        """.trimMargin(),
    )

    try {
      tolgeeEmailSender.sendEmail(params)
    } catch (e: Exception) {
      logger.error(e.message)
      Sentry.captureException(e)
    }
  }

  private fun composeEmailSubject(notification: Notification) = when (notification.type) {
    NotificationType.TASK_ASSIGNED -> "New task assignment"
    NotificationType.TASK_COMPLETED -> "Task has been completed"
    NotificationType.TASK_CLOSED -> "Task has been closed"
    NotificationType.MFA_ENABLED -> "Multi-factor authentication has been enabled for your account"
    NotificationType.MFA_DISABLED -> "Multi-factor authentication has been enabled for your account"
    NotificationType.PASSWORD_CHANGED -> "Password has been changed for your account"
  }

  private fun composeEmailText(notification: Notification) =
    when (notification.type) {
      NotificationType.TASK_ASSIGNED, NotificationType.TASK_COMPLETED, NotificationType.TASK_CLOSED -> {
        val task =
          notification.linkedTask
            ?: throw IllegalStateException(
              "Notification of type ${notification.type} must contain linkedTask.",
            )
        val header = when (notification.type) {
          NotificationType.TASK_ASSIGNED -> "You've been assigned to a task"
          NotificationType.TASK_COMPLETED -> "Task you've created has been completed"
          NotificationType.TASK_CLOSED -> "Task you've created has been closed"
          else -> throw IllegalStateException("Non-task notification detected: ${notification.type}")
        }

        """
          |$header:
          |<br/>
          |${getTaskLinkHtml(task)}
          |<br/><br/>
          |Check all your tasks <a href="${getMyTasksUrl()}">here</a>.
        """.trimMargin()
      }
      NotificationType.MFA_ENABLED, NotificationType.MFA_DISABLED -> {
        val enabledDisabled = if (notification.type == NotificationType.MFA_ENABLED) "enabled" else "disabled"
        """
          |Multi-factor authentication has been $enabledDisabled for your account.
          |<br/><br/>
          |Check your security settings <a href="${frontendUrlProvider.url}/account/security"}">here</a>.
        """.trimMargin()
      }
      NotificationType.PASSWORD_CHANGED -> {
        """
          |Password has been changed for your account.
          |<br/><br/>
          |Check your security settings <a href="${frontendUrlProvider.url}/account/security"}">here</a>.
        """.trimMargin()
      }
    }

  private fun getTaskLinkHtml(task: Task) =
    """
      |<a href="${getTaskUrl(task.project.id, task.number)}">
      |  ${task.name} #${task.number} (${task.language.name}) - ${getTaskTypeName(task.type)}
      |</a>
    """.trimMargin()

  private fun getTaskTypeName(type: TaskType): String =
    if (type === TaskType.TRANSLATE) {
      "translate"
    } else {
      "review"
    }

  fun getTaskUrl(
    projectId: Long,
    taskId: Long,
  ): String = "${frontendUrlProvider.url}/projects/$projectId/task?number=$taskId&detail=true"

  fun getMyTasksUrl(): String = "${frontendUrlProvider.url}/my-tasks"
}
