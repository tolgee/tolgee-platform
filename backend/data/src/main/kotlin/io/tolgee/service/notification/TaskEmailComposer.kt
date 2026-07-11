package io.tolgee.service.notification

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.model.notifications.Notification
import io.tolgee.model.task.Task
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class TaskEmailComposer(
  private val frontendUrlProvider: FrontendUrlProvider,
  private val i18n: I18n,
) : EmailComposer {
  override fun composeEmail(notification: Notification): String {
    val task =
      notification.linkedTask
        ?: throw IllegalStateException(
          "Notification of type ${notification.type} must contain linkedTask.",
        )
    return """
        |${i18n.translate("notifications.email.task-header.${notification.type}", taskLink(task), taskType(task))}
        |<br/><br/>
        |${viewInTolgeeLink(task)}
        |<br/><br/>
        |${checkAllYourTasksFooter()}
      """.trimMargin()
  }

  private fun checkAllYourTasksFooter() =
    i18n
      .translate("notifications.email.my-tasks-link", frontendUrlProvider.getMyTasksUrl())

  private fun taskLink(task: Task): String {
    return """
        |<a href="${taskUrl(task)}">
        |  ${taskName(task.name)} #${task.number} (${task.language.name})
        |</a>
      """.trimMargin()
  }

  private fun viewInTolgeeLink(task: Task): String =
    """
      |<a href="${taskUrl(task)}">
      |  ${i18n.translate("notifications.email.view-in-tolgee-link")}
      |</a>
    """.trimMargin()

  private fun taskUrl(task: Task) = frontendUrlProvider.getTaskUrl(task.project.id, task.number)

  private fun taskType(task: Task): String = i18n.translate("notifications.email.task-type.${task.type}")

  fun taskName(name: String?): String {
    return if (name.isNullOrBlank()) "Task" else name
  }
}
