package io.tolgee.service.notification.activity

import io.tolgee.component.LockingProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.service.notification.NotificationSettingsService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.Duration

@Component
class ActivityNotificationProcessor(
  private val queue: ActivityNotificationQueue,
  private val classifier: RevisionNotificationClassifier,
  private val recipientResolver: NotificationRecipientResolver,
  private val writer: GroupedNotificationWriter,
  private val settingsService: NotificationSettingsService,
  private val lockingProvider: LockingProvider,
  private val transactionManager: PlatformTransactionManager,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val tolgeeProperties: TolgeeProperties,
) : Logging {
  @Scheduled(
    fixedDelayString = "\${tolgee.notifications.processing-interval-ms:30000}",
    initialDelayString = "\${tolgee.notifications.processing-interval-ms:30000}",
  )
  fun processQueue() {
    lockingProvider.withLockingIfFree(LOCK_NAME, LEASE_TIME) {
      val deadline = System.currentTimeMillis() + LEASE_TIME.toMillis() * 4 / 5
      while (System.currentTimeMillis() < deadline) {
        val batch = queue.takeBatch(tolgeeProperties.notifications.processingBatchSize)
        if (batch.isEmpty()) break
        batch.forEach { processMarker(it.revisionId) }
      }
    }
  }

  private fun processMarker(revisionId: Long) {
    try {
      executeInNewTransaction(transactionManager) {
        val classified = classifier.classify(revisionId)
        if (classified != null) {
          writeNotifications(classified)
        }
        queue.delete(revisionId)
      }
    } catch (e: Exception) {
      logger.warn("Processing notification marker for revision $revisionId failed", e)
      val dropped =
        executeInNewTransaction(transactionManager) {
          queue.recordFailure(revisionId, tolgeeProperties.notifications.maxAttempts)
        }
      if (dropped) {
        logger.error("Dropping notification marker for revision $revisionId after repeated failures", e)
      }
    }
  }

  private fun writeNotifications(classified: ClassifiedRevision) {
    val recipients = recipientResolver.resolve(classified)
    recipients.groupBy { it.type }.forEach { (type, forType) ->
      val userIds = forType.map { it.userId }
      val inApp = settingsService.usersWithEnabled(userIds, type, NotificationChannel.IN_APP)
      val digest = settingsService.usersWithEnabled(userIds, type, NotificationChannel.EMAIL)
      forType.filter { it.userId in inApp }.forEach { recipient ->
        val notification =
          writer.write(
            userId = recipient.userId,
            projectId = classified.projectId,
            originatingUserId = classified.authorId,
            type = type,
            entityIds = recipient.entityIds,
            digestEnabled = recipient.userId in digest,
          )
        applicationEventPublisher.publishEvent(OnNotificationsChangedForUser(recipient.userId, notification))
      }
    }
  }

  companion object {
    private const val LOCK_NAME = "notification-activity-processing"
    private val LEASE_TIME = Duration.ofMinutes(5)
  }
}
