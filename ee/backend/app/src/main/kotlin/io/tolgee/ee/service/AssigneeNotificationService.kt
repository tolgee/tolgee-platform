package io.tolgee.ee.service

import io.tolgee.service.notification.EmailNotificationsService
import org.springframework.stereotype.Component

@Component
class AssigneeNotificationService(
  private val emailNotificationsService: EmailNotificationsService,
) {
  @Deprecated(
    "Use EmailNotificationsService.getTaskUrl directly, " +
      "this is kept here only for backwards compatibility with billing.",
  )
  fun getTaskUrl(
    projectId: Long,
    taskId: Long,
  ): String = emailNotificationsService.getTaskUrl(projectId, taskId)
}
