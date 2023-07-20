package io.tolgee.component.reporting

import io.tolgee.events.OnProjectActivityEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class BusinessEventReportingActivityListener(
  private val applicationEventPublisher: ApplicationEventPublisher,
) {
  @EventListener
  fun listen(event: OnProjectActivityEvent) {
    val userId = event.activityHolder.activityRevision?.authorId ?: return
    val activityName = event.activityHolder.activityRevision?.type?.name ?: return
    val projectId = event.activityHolder.activityRevision?.projectId ?: return

    applicationEventPublisher.publishEvent(
      OnBusinessEventToCaptureEvent(
        eventName = activityName,
        userAccountId = userId,
        projectId = projectId,
        utmData = event.activityHolder.utmData,
        data = event.activityHolder.sdkInfo
      )
    )
  }
}
