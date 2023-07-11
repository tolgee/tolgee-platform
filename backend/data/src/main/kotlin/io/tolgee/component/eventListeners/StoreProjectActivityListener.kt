package io.tolgee.component.eventListeners

import io.tolgee.activity.ActivityService
import io.tolgee.events.OnProjectActivityEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StoreProjectActivityListener(
  private var activityService: ActivityService
) {
  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    activityService.storeActivityData(event.activityRevision, event.modifiedEntities)
  }
}
