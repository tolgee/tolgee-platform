package io.tolgee.service.notification

import io.tolgee.model.notifications.NotificationDigestFrequency
import io.tolgee.model.notifications.NotificationDigestState
import io.tolgee.repository.notification.NotificationDigestStateRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@Service
class NotificationDigestStateService(
  private val repository: NotificationDigestStateRepository,
  private val entityManager: EntityManager,
) {
  fun getFrequency(userId: Long): NotificationDigestFrequency =
    repository.findById(userId).orElse(null)?.frequency ?: NotificationDigestFrequency.DAILY

  @Transactional
  fun setFrequency(
    userId: Long,
    frequency: NotificationDigestFrequency,
  ) {
    repository.upsertFrequency(userId, frequency.name)
    refreshLoaded(userId)
  }

  @Transactional
  fun resetWindow(userId: Long) {
    repository.resetWindow(userId)
  }

  @Transactional
  fun markSent(
    userId: Long,
    at: Date,
  ) {
    repository.upsertLastDigestSentAt(userId, at)
    refreshLoaded(userId)
  }

  private fun refreshLoaded(userId: Long) {
    repository.findById(userId).ifPresent { entityManager.refresh(it) }
  }

  fun find(userIds: Collection<Long>): Map<Long, NotificationDigestState> =
    repository.findAllById(userIds).associateBy { it.userId }
}
