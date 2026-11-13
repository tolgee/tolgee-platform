package io.tolgee.service.notification

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.notification.NotificationFilters
import io.tolgee.dtos.response.CursorValue
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationEmailTiming
import io.tolgee.model.notifications.NotificationTypeGroup
import io.tolgee.repository.notification.NotificationRepository
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.util.Date

@Service
class NotificationService(
  private val notificationRepository: NotificationRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val emailNotificationsService: EmailNotificationsService,
  private val notificationSettingsService: NotificationSettingsService,
  private val currentDateProvider: CurrentDateProvider,
  private val tolgeeProperties: TolgeeProperties,
) {
  fun getNotifications(
    userId: Long,
    pageable: Pageable = Pageable.unpaged(),
    filters: NotificationFilters = NotificationFilters(),
    cursor: Map<String, CursorValue>? = null,
  ): Page<Notification> =
    notificationRepository.fetchNotificationsByUserId(
      userId,
      pageable,
      filters,
      cursor?.get("updatedAt")?.value?.let { Timestamp.from(Instant.ofEpochMilli(it.toLong())) },
      cursor?.get("id")?.value?.toLong(),
    )

  fun getCountOfUnseenNotifications(userId: Long): Int =
    notificationRepository
      .fetchNotificationsByUserId(
        userId,
        PageRequest.of(0, 1),
        NotificationFilters(false),
      ).totalElements
      .toInt()

  fun findVisibleToUser(
    notificationId: Long,
    userId: Long,
  ): Notification? = notificationRepository.findVisibleToUser(notificationId, userId)

  @Transactional
  fun deleteNotificationsOfUser(userId: Long) {
    notificationRepository.deleteAllOfUser(userId)
  }

  @Transactional
  fun deleteUnseenMentionsOfCommentExcept(
    commentId: Long,
    keepUserIds: Collection<Long>,
  ) {
    notificationRepository.deleteUnseenMentionsOfCommentExcept(commentId, keepUserIds)
  }

  @Transactional
  fun notify(notification: Notification) {
    requireLinkedEntity(notification)
    val inApp = notificationSettingsService.getSettingValue(notification, NotificationChannel.IN_APP)
    val email = notificationSettingsService.getSettingValue(notification, NotificationChannel.EMAIL)
    when (notification.type.emailTiming) {
      NotificationEmailTiming.IMMEDIATE -> sendEmailNow(notification, email)
      NotificationEmailTiming.MENTION_DELAYED -> scheduleMentionEmail(notification, email, inApp)
      NotificationEmailTiming.WORK_GROUPED -> Unit
    }
    if (inApp) {
      saveAndPublish(notification)
      return
    }
    if (notification.emailPending) notificationRepository.save(notification)
  }

  private fun requireLinkedEntity(notification: Notification) {
    if (notification.type.group == NotificationTypeGroup.TASKS && notification.linkedTask == null) {
      throw IllegalArgumentException("Task notification must have a linked task")
    }
    if (notification.type.group == NotificationTypeGroup.COMMENTS && notification.linkedComment == null) {
      throw IllegalArgumentException("Comment notification must have a linked comment")
    }
  }

  private fun sendEmailNow(
    notification: Notification,
    emailEnabled: Boolean,
  ) {
    if (!emailEnabled) return
    emailNotificationsService.sendEmailNotification(notification)
  }

  private fun scheduleMentionEmail(
    notification: Notification,
    emailEnabled: Boolean,
    inAppEnabled: Boolean,
  ) {
    if (!emailEnabled || !tolgeeProperties.smtp.hasHostAndSender()) return
    notification.emailPending = true
    notification.emailOnly = !inAppEnabled
    notification.emailDueAt =
      Date(currentDateProvider.date.time + tolgeeProperties.notifications.mentionEmailDelayMs)
  }

  private fun saveAndPublish(notification: Notification) {
    notificationRepository.save(notification)
    applicationEventPublisher.publishEvent(
      OnNotificationsChangedForUser(
        notification.user.id,
        notification,
      ),
    )
  }

  @Transactional
  fun markNotificationsAsSeen(
    notificationIds: List<Long>,
    userId: Long,
  ) {
    val modifiedCount =
      notificationRepository.markNotificationsAsSeen(notificationIds, userId, currentDateProvider.date)

    if (modifiedCount > 0) {
      applicationEventPublisher.publishEvent(
        OnNotificationsChangedForUser(
          userId,
        ),
      )
    }
  }
}
