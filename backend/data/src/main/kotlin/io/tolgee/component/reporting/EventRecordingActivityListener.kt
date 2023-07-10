package io.tolgee.component.reporting

import io.tolgee.events.OnProjectActivityStoredEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class EventRecordingActivityListener(
  private val applicationEventPublisher: ApplicationEventPublisher
) {
  @EventListener
  @Async
  fun listen(event: OnProjectActivityStoredEvent) {
    val userId = event.activityRevision.authorId ?: return
    val activityName = event.activityRevision.type?.name ?: return
    val projectId = event.activityRevision.projectId ?: return

    applicationEventPublisher.publishEvent(
      OnEventToCaptureEvent(
        eventName = activityName,
        userAccountId = userId,
        projectId = projectId
      )
    )
  }
}
