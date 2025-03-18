package io.tolgee.ee.component

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.ee.service.EeSubscriptionServiceImpl
import io.tolgee.ee.service.NoActiveSubscriptionException
import io.tolgee.events.OnUserCountChanged
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EeOnUserCountChangedListener(
  private val eeSubscriptionService: EeSubscriptionServiceImpl,
  private val userAccountService: UserAccountService,
  private val billingConfProvider: PublicBillingConfProvider,
) : Logging {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onUserCountChanged(event: OnUserCountChanged) {
    try {
      // we don't want this to happen on Tolgee Cloud
      if (billingConfProvider().enabled) {
        return
      }

      val seats = userAccountService.countAllEnabled()
      val subscription = eeSubscriptionService.findSubscriptionDto()
      eeSubscriptionService.reportUsage(subscription = subscription, seats = seats)
      if (!event.decrease) {
        eeSubscriptionService.checkUserCount(subscription, seats)
      }
    } catch (e: NoActiveSubscriptionException) {
      logger.debug("No active subscription, skipping usage reporting.")
    }
  }
}
