package io.tolgee.service.notification.activity

import io.tolgee.component.LockingProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.events.OnNotificationsChangedForUser
import io.tolgee.model.notifications.NotificationChannel
import io.tolgee.model.notifications.NotificationType
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
      var lastRevisionId: Long? = null
      while (true) {
        val batch = queue.takeBatch(tolgeeProperties.notifications.processingBatchSize, lastRevisionId)
        if (batch.isEmpty()) return@withLockingIfFree
        for (markersOfProject in batch.groupBy { it.projectId }.values) {
          if (System.currentTimeMillis() >= deadline) return@withLockingIfFree
          processMarkers(markersOfProject)
        }
        lastRevisionId = batch.last().revisionId
      }
    }
  }

  private fun processMarkers(markers: List<QueuedMarker>) {
    try {
      executeInNewTransaction(transactionManager) {
        writeNotifications(markers.mapNotNull { classifier.classify(it.revisionId) })
        markers.forEach { queue.delete(it.revisionId) }
      }
    } catch (e: Exception) {
      if (markers.size > 1) {
        logger.warn(
          "Processing ${markers.size} notification markers together failed, retrying one by one: ${e.message}",
        )
        markers.forEach { processMarkers(listOf(it)) }
        return
      }
      recordFailure(markers.single().revisionId, e)
    }
  }

  private fun recordFailure(
    revisionId: Long,
    e: Exception,
  ) {
    val dropped =
      executeInNewTransaction(transactionManager) {
        queue.recordFailure(revisionId, tolgeeProperties.notifications.maxAttempts)
      }
    if (dropped) {
      logger.error("Dropping notification marker for revision $revisionId after repeated failures", e)
      return
    }
    logger.warn("Processing notification marker for revision $revisionId failed: ${e.message}")
  }

  private class PendingNotification(
    var originatingUserId: Long?,
    val entityIds: LinkedHashSet<Long> = LinkedHashSet(),
  )

  private fun writeNotifications(revisionsOfProject: List<ClassifiedRevision>) {
    val first = revisionsOfProject.firstOrNull() ?: return
    val projectMembers = recipientResolver.loadProjectMembers(first.projectId, first.baseLanguageId) ?: return

    val pending = LinkedHashMap<Pair<Long, NotificationType>, PendingNotification>()
    revisionsOfProject.forEach { revision ->
      recipientResolver.resolve(revision, projectMembers).forEach { recipient ->
        val notification =
          pending.getOrPut(
            recipient.userId to recipient.type,
          ) { PendingNotification(revision.authorId) }
        notification.originatingUserId = revision.authorId
        notification.entityIds.addAll(recipient.entityIds)
      }
    }

    pending.entries.groupBy { it.key.second }.forEach { (type, ofType) ->
      val userIds = ofType.map { it.key.first }
      val inApp = settingsService.usersWithEnabled(userIds, type, NotificationChannel.IN_APP)
      val digest = settingsService.usersWithEnabled(userIds, type, NotificationChannel.EMAIL)
      ofType.filter { it.key.first in inApp }.forEach { (key, notification) ->
        val userId = key.first
        val written =
          writer.write(
            userId = userId,
            projectId = first.projectId,
            originatingUserId = notification.originatingUserId,
            type = type,
            entityIds = notification.entityIds.take(tolgeeProperties.notifications.entityCap),
            digestEnabled = userId in digest,
          )
        applicationEventPublisher.publishEvent(OnNotificationsChangedForUser(userId, written))
      }
    }
  }

  companion object {
    private const val LOCK_NAME = "notification-activity-processing"
    private val LEASE_TIME = Duration.ofMinutes(5)
  }
}
