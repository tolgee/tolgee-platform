package io.tolgee.ee.service

import io.tolgee.component.FrontendUrlProvider
import org.springframework.stereotype.Component

@Component
class AssigneeNotificationService(
  private val frontendUrlProvider: FrontendUrlProvider,
) {
  fun notifyNewAssignee(
    user: UserAccount,
    task: Task,
  ) {
    val taskUrl = getTaskUrl(task.project.id, task.number)
    val myTasksUrl = getMyTasksUrl()

    val params =
      EmailParams(
        to = user.username,
        subject = "New task assignment",
        text =
          """
          Hello! 👋<br/><br/>          
          You've been assigned to a task:<br/>
          <a href="$taskUrl">${getTaskName(task.name)} #${task.number} (${task.language.name}) - ${
            getTaskTypeName(task.type)
          }</a><br/><br/>
          
          
          Check all your tasks <a href="$myTasksUrl">here</a>.<br/><br/>
          
          Regards,<br/>
          Tolgee
          """.trimIndent(),
      )

    try {
      tolgeeEmailSender.sendEmail(params)
    } catch (e: Exception) {
      logger.error(e.message)
      Sentry.captureException(e)
    }
  }

  private fun getTaskTypeName(type: TaskType): String {
    return if (type === TaskType.TRANSLATE) {
      "translate"
    } else {
      "review"
    }
  }

  @Deprecated(
    "Use FrontendUrlProvider.getTaskUrl directly, " +
      "this is kept here only for backwards compatibility with billing.",
  )
  fun getTaskUrl(
    projectId: Long,
    taskId: Long,
  ): String {
    return "${frontendUrlProvider.url}/projects/$projectId/task?number=$taskId&detail=true"
  }

  fun getMyTasksUrl(): String {
    return "${frontendUrlProvider.url}/my-tasks"
  }

  fun getTaskName(name: String?): String {
    return if (name.isNullOrBlank()) "Task" else name
  }
}
