package io.tolgee.service.notification.activity

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.ALLOCATION_SIZE
import io.tolgee.model.SEQUENCE_NAME
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationSettingsService
import io.tolgee.util.SequenceIdProvider
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationFanoutService(
  private val classifier: RevisionNotificationClassifier,
  private val recipientResolver: NotificationRecipientResolver,
  private val writer: GroupedNotificationWriter,
  private val settingsService: NotificationSettingsService,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val tolgeeProperties: TolgeeProperties,
) {
  @Transactional
  fun fanOut(revisionId: Long) {
    val revision = classifier.classify(revisionId) ?: return
    val idProvider = SequenceIdProvider(SEQUENCE_NAME, ALLOCATION_SIZE)
    inLockOrder(recipientResolver.resolve(revision))
      .groupBy { it.type }
      .forEach { (type, recipients) -> writeType(revision, type, recipients, idProvider) }
  }

  private fun writeType(
    revision: ClassifiedRevision,
    type: NotificationType,
    recipients: List<RecipientNotification>,
    idProvider: SequenceIdProvider,
  ) {
    val userIds = recipients.map { it.userId }
    val inApp = settingsService.usersWithEnabled(userIds, type, NotificationChannel.IN_APP)
    val email = settingsService.usersWithEnabled(userIds, type, NotificationChannel.EMAIL)
    recipients.filter { it.userId in inApp }.forEach { recipient ->
      val request =
        GroupedNotificationWrite(
          userId = recipient.userId,
          projectId = revision.projectId,
          originatingUserId = revision.authorId,
          type = type,
          entityIds = recipient.entityIds.distinct().take(tolgeeProperties.notifications.entityCap),
          emailEnabled = recipient.userId in email,
          revisionIds = listOf(revision.revisionId),
        )
      val written = writer.write(request, idProvider)
      applicationEventPublisher.publishEvent(OnNotificationsChangedForUser(recipient.userId, written))
    }
  }
}

// Concurrent fan-out jobs upsert rows in the same order, so they wait on each other instead of deadlocking.
internal fun inLockOrder(recipients: List<RecipientNotification>): List<RecipientNotification> =
  recipients.sortedWith(compareBy({ it.type.ordinal }, { it.userId }))
