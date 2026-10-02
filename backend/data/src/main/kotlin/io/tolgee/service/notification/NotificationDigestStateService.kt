package io.tolgee.service.notification

import io.tolgee.model.notifications.NotificationDigestFrequency
import io.tolgee.model.notifications.NotificationDigestState
import io.tolgee.repository.notification.NotificationDigestStateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@Service
class NotificationDigestStateService(
  private val repository: NotificationDigestStateRepository,
) {
  fun getFrequency(userId: Long): NotificationDigestFrequency =
    repository.findById(userId).orElse(null)?.frequency ?: NotificationDigestFrequency.DAILY

  @Transactional
  fun setFrequency(
    userId: Long,
    frequency: NotificationDigestFrequency,
  ) {
    val state = repository.findById(userId).orElse(null) ?: NotificationDigestState(userId = userId)
    state.frequency = frequency
    repository.save(state)
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
    val state = repository.findById(userId).orElse(null) ?: NotificationDigestState(userId = userId)
    state.lastDigestSentAt = at
    repository.save(state)
  }

  fun find(userIds: Collection<Long>): Map<Long, NotificationDigestState> =
    repository.findAllById(userIds).associateBy { it.userId }
}
