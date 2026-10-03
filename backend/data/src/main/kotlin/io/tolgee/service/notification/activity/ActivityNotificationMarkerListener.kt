package io.tolgee.service.notification.activity

import io.tolgee.batch.events.OnBatchJobFinalized
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ActivityNotificationMarkerListener(
  private val queue: ActivityNotificationQueue,
) {
  @EventListener
  fun onBatchJobFinalized(event: OnBatchJobFinalized) {
    val revisionId = event.activityRevisionId ?: return
    queue.add(revisionId)
  }
}
