package io.tolgee.ee.component

import io.tolgee.batch.OnBatchJobCompleted
import io.tolgee.batch.events.OnBatchJobCancelled
import io.tolgee.batch.events.OnBatchJobFailed
import io.tolgee.batch.events.OnBatchJobSucceeded
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class BatchJobNotificationListener(
  private val notificationService: NotificationService,
  private val userAccountService: UserAccountService,
  private val entityManager: EntityManager,
) : Logging {
  @TransactionalEventListener(OnBatchJobSucceeded::class, phase = TransactionPhase.AFTER_COMMIT)
  fun onBatchJobSucceeded(event: OnBatchJobSucceeded) {
    sendNotification(event)
  }

  @TransactionalEventListener(OnBatchJobFailed::class)
  fun onBatchJobFailed(event: OnBatchJobFailed) {
    sendNotification(event)
  }

  @TransactionalEventListener(OnBatchJobCancelled::class)
  fun onBatchJobCancelled(event: OnBatchJobCancelled) {
    sendNotification(event)
  }

  private fun sendNotification(event: OnBatchJobCompleted) {
    val authorId = event.job.authorId ?: return
    val projectId = event.job.projectId ?: return

    val author = userAccountService.findDto(authorId) ?: return
    val userEntity = entityManager.getReference(io.tolgee.model.UserAccount::class.java, authorId)
    val projectEntity = entityManager.getReference(io.tolgee.model.Project::class.java, projectId)
    val batchJobEntity = entityManager.getReference(io.tolgee.model.batch.BatchJob::class.java, event.job.id)

    try {
      notificationService.notify(
        Notification().apply {
          type = NotificationType.BATCH_JOB_FINISHED
          user = userEntity
          project = projectEntity
          linkedBatchJob = batchJobEntity
        },
      )
    } catch (e: Exception) {
      logger.warn("Failed to send batch job notification for job ${event.job.id}: ${e.message}")
    }
  }
}
