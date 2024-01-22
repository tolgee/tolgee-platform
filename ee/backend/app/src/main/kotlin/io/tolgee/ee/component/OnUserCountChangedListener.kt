package io.tolgee.ee.component

import io.tolgee.ee.service.EeSubscriptionService
import io.tolgee.events.OnUserCountChanged
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OnUserCountChangedListener(
  private val eeSubscriptionService: EeSubscriptionService,
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onUserCountChanged(event: OnUserCountChanged) {
    eeSubscriptionService.reportUsage()
  }
}
