package io.tolgee.ee.component

import io.tolgee.ee.service.EeSubscriptionService
import io.tolgee.events.OnUserCountChanged
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class OnUserCountChangedListener(
  private val eeSubscriptionService: EeSubscriptionService
) {

  @EventListener
  fun onUserCountChanged(event: OnUserCountChanged) {
    eeSubscriptionService.reportUsage()
  }
}
