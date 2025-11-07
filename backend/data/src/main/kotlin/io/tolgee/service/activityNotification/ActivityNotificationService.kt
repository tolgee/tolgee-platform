package io.tolgee.service.activityNotification

import io.tolgee.activity.data.ActivityType
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.ActivityNotificationBatchRequest
import io.tolgee.batch.request.ActivityNotificationRequest
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class ActivityNotificationService(
  val notificationService: NotificationService,
  val userAccountService: UserAccountService,
  val projectService: ProjectService,
  val batchJobService: BatchJobService,
) {
  fun createBatchJob(
    activityRevision: ActivityRevision,
    modifiedEntity: ActivityModifiedEntity,
  ) {
    val projectId = activityRevision.projectId ?: return
    val activityType = activityRevision.type ?: return
    val originatingUserId = activityRevision.authorId ?: return

    val batchJobType =
      when (activityType) {
        ActivityType.CREATE_KEY -> BatchJobType.NOTIFY_KEY_CREATED
        ActivityType.SET_TRANSLATIONS -> BatchJobType.NOTIFY_STRING_TRANSLATED
        ActivityType.SET_TRANSLATION_STATE -> BatchJobType.NOTIFY_STRING_REVIEWED
        else -> return
      }

    val request =
      ActivityNotificationRequest(
        projectId = projectId,
        originatingUserId = originatingUserId,
        entityId = modifiedEntity.entityId,
        activityType = activityType,
      )

    val batchRequest = ActivityNotificationBatchRequest(items = listOf(request))

    batchJobService.startJob(
      request = batchRequest,
      project = projectService.get(projectId),
      author = userAccountService.get(originatingUserId),
      type = batchJobType,
      isHidden = false,
      debounceDuration = Duration.ofSeconds(5), // TODO: Change to 5 minutes
      debouncingKeyProvider = { params -> "${batchJobType.name}:${params.projectId}" },
    )
  }

  fun sendActivityNotifications(
    projectId: Long,
    originatingUserId: Long,
    activityType: ActivityType,
    entityIds: List<Long>,
  ) {
    val notification = Notification()
    notification.type =
      when (activityType) {
        ActivityType.CREATE_KEY, ActivityType.ACTIVITY_NOTIFICATION_KEY_CREATED,
        -> NotificationType.KEY_CREATED
        ActivityType.SET_TRANSLATIONS, ActivityType.ACTIVITY_NOTIFICATION_STRING_TRANSLATED,
        -> NotificationType.STRING_TRANSLATED
        ActivityType.SET_TRANSLATION_STATE, ActivityType.ACTIVITY_NOTIFICATION_STRING_REVIEWED,
        -> NotificationType.STRING_REVIEWED
        else -> throw IllegalArgumentException("Unsupported activity type for notification")
      }

    // TODO: This is just for testing purposes. Here we need to properly handle notifications.

    notification.originatingUser = userAccountService.get(originatingUserId)
    notification.user = userAccountService.get(originatingUserId)
    notification.project = projectService.get(projectId)

    notificationService.notify(notification)
  }
}
