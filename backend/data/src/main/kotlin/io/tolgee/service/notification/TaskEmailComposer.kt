package io.tolgee.service.notification

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.model.Notification
import io.tolgee.model.enums.TaskType
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class TaskEmailComposer(
  private val frontendUrlProvider: FrontendUrlProvider,
  private val i18n: I18n,
) : EmailComposer {
  override fun composeEmail(notification: Notification): String =
    """
      |${i18n.translate("notifications.email.task-header.${notification.type}")}:
      |<br/>
      |${taskLink(notification)}
      |<br/><br/>
      |${checkAllYourTasksFooter()}
    """.trimMargin()

  private fun checkAllYourTasksFooter() =
    i18n
      .translate("notifications.email.my-tasks-link", frontendUrlProvider.getMyTasksUrl())

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
}
