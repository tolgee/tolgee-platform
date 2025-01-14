package io.tolgee.ee.service

import io.sentry.Sentry
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.component.email.TolgeeEmailSender
import io.tolgee.dtos.misc.EmailParams
import io.tolgee.model.Notification
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TaskType
import io.tolgee.model.task.Task
import io.tolgee.service.notification.NotificationService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component

@Component
class AssigneeNotificationService(
  private val tolgeeEmailSender: TolgeeEmailSender,
  private val frontendUrlProvider: FrontendUrlProvider,
  private val notificationService: NotificationService,
) : Logging {
  fun notifyNewAssignee(
    user: UserAccount,
    task: Task,
  ) {
    notificationService.save(
      Notification().apply {
        this.user = user
        this.linkedTask = task
        this.project = task.project
        this.originatingUser = task.author
      },
    )
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
          <a href="$taskUrl">${task.name} #${task.number} (${task.language.name}) - ${
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

  fun getTaskUrl(
    projectId: Long,
    taskId: Long,
  ): String {
    return "${frontendUrlProvider.url}/projects/$projectId/task?number=$taskId&detail=true"
  }

  fun getMyTasksUrl(): String {
    return "${frontendUrlProvider.url}/my-tasks"
  }
}
