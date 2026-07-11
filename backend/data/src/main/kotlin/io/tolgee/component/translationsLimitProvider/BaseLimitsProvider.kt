package io.tolgee.component.translationsLimitProvider

import io.tolgee.dtos.UsageLimits
import org.springframework.stereotype.Component

@Component
class BaseLimitsProvider : LimitsProvider {
  override fun getLimits(organizationId: Long): UsageLimits {
    return UsageLimits(
      isPayAsYouGo = false,
      isTrial = false,
      strings =
        UsageLimits.Limit(
          included = -1,
          limit = -1,
        ),
      keys =
        UsageLimits.Limit(
          included = -1,
          limit = -1,
        ),
      seats =
        UsageLimits.Limit(
          included = -1,
          limit = -1,
        ),
      mtCreditsInCents =
        UsageLimits.Limit(
          included = -1,
          limit = -1,
        ),
    )
  }
}
