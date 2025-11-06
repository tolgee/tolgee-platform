package io.tolgee.service.activityNotification

import io.tolgee.activity.data.ActivityType
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.service.notification.NotificationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Service

@Service
class ActivityNotificationService(
  val notificationService: NotificationService,
  val userAccountService: UserAccountService,
  val projectService: ProjectService,
) {
  fun createBatchJob(
    activityRevision: ActivityRevision,
    modifiedEntity: ActivityModifiedEntity,
  ) {
    // TODO: Remake - This function won't do, what it's doing now :)
    val keyId = modifiedEntity.entityId
    val projectId = activityRevision.projectId ?: return
    val activityType = activityRevision.type ?: return
    val originatingUserId = activityRevision.authorId ?: return

    val notification =
      io.tolgee.model.notifications
        .Notification()
    notification.type =
      when (activityType) {
        ActivityType.CREATE_KEY -> io.tolgee.model.notifications.NotificationType.KEY_CREATED
        ActivityType.SET_TRANSLATIONS -> io.tolgee.model.notifications.NotificationType.STRING_TRANSLATED
        ActivityType.SET_TRANSLATION_STATE -> io.tolgee.model.notifications.NotificationType.STRING_REVIEWED
        else -> throw IllegalArgumentException("Unsupported activity type for notification")
      }
    notification.originatingUser = userAccountService.get(originatingUserId)
    notification.user = userAccountService.get(originatingUserId)
    notification.project = projectService.get(projectId)

    notificationService.notify(notification)
  }
}
