package io.tolgee.ee.component.limitsAndReporting

import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.ee.service.NoActiveSubscriptionException
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.ee.service.eeSubscription.usageReporting.UsageReportingService
import io.tolgee.events.OnUserCountChanged
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Listens for changes in user count and reports usage to Tolgee Cloud.
 *
 * This listener uses the deferred reporting mechanism, which means that if multiple
 * user count changes occur within a short time period (less than 1 minute), only one
 * report will be sent to Tolgee Cloud, reducing API calls.
 */
@Component
class EeOnUserCountChangedListener(
  private val eeSubscriptionService: EeSubscriptionServiceImpl,
  private val userAccountService: UserAccountService,
  private val billingConfProvider: PublicBillingConfProvider,
  private val usageReportingService: UsageReportingService,
) : Logging {
  /**
   * Handles user count change events by reporting the current seat usage.
   *
   * When the number of users changes, this method reports the new count to Tolgee Cloud
   * using the deferred reporting mechanism, which may delay the actual API call by up to 1 minute
   * if another report was sent recently.
   */
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onUserCountChanged(event: OnUserCountChanged) {
    try {
      // we don't want this to happen on Tolgee Cloud
      if (billingConfProvider().enabled) {
        return
      }

      val seats = userAccountService.countAllEnabled()
      val subscription = eeSubscriptionService.findSubscriptionDto()
      usageReportingService.reportUsage(subscription = subscription, seats = seats)
      if (!event.decrease) {
        eeSubscriptionService.checkSeatCount(seats)
      }
    } catch (_: NoActiveSubscriptionException) {
      logger.debug("No active subscription, skipping usage reporting.")
    }
  }
}
