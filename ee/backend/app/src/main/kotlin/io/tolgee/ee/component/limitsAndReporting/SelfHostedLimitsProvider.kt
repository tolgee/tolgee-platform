package io.tolgee.ee.component.limitsAndReporting

import io.tolgee.dtos.UsageLimits
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

/**
 * Provides subscription limits stored on Self Hosted instance
 *
 */
@Component
class SelfHostedLimitsProvider(
  @Lazy
  private val eeSubscriptionServiceImpl: EeSubscriptionServiceImpl,
) {
  fun getLimits(): UsageLimits {
    val subscription = eeSubscriptionServiceImpl.findSubscriptionDto() ?: return DEFAULT_LIMITS
    return UsageLimits(
      isPayAsYouGo = subscription.isPayAsYouGo,
      keys = UsageLimits.Limit(included = subscription.includedKeys, limit = subscription.keysLimit),
      seats = UsageLimits.Limit(included = subscription.includedSeats, limit = subscription.seatsLimit),
      strings = DEFAULT_LIMITS.strings,
      mtCreditsInCents = DEFAULT_LIMITS.mtCreditsInCents,
      isTrial = false,
    )
  }

  companion object {
    private val DEFAULT_LIMITS =
      UsageLimits(
        keys = UsageLimits.Limit(included = -1, limit = -1),
        seats = UsageLimits.Limit(included = 10, limit = 10),
        strings = UsageLimits.Limit(included = -1, limit = -1),
        mtCreditsInCents = UsageLimits.Limit(included = -1, limit = -1),
        isPayAsYouGo = false,
        isTrial = false,
      )
  }
}
