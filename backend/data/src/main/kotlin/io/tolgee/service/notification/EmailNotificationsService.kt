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
    val params = text?.let { // TODO remove nullable
      EmailParams(
        to = notification.user.username,
        subject = subject!!, // TODO remove nullable
        text =
          """
        |Hello!👋
        |<br/><br/>
        |$it
        |<br/><br/>
        |Regards,<br/>
        |Tolgee
        """.trimMargin(),
      )
    }

    try {
      params?.let { tolgeeEmailSender.sendEmail(it) } // TODO remove nullable
    } catch (e: Exception) {
      logger.error(e.message)
      Sentry.captureException(e)
    }
  }

  private fun composeEmailSubject(notification: Notification) = when (notification.type) {
    NotificationType.TASK_ASSIGNED -> "New task assignment"
    else -> null // TODO remove else branch
  }

  private fun composeEmailText(notification: Notification) =
    when (notification.type) {
      NotificationType.TASK_ASSIGNED -> {
        val task =
          notification.linkedTask
            ?: throw IllegalStateException(
              "Notification of type ${notification.type} must contain linkedTask.",
            )

        """
        |You've been assigned to a task:
        |<br/>
        |${getTaskLinkHtml(task)}
        |<br/><br/>
        |Check all your tasks <a href="${getMyTasksUrl()}">here</a>.
        """.trimMargin()
      }

      else -> null // TODO add branches for all types, remove else
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
