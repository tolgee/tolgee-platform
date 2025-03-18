package io.tolgee.component.translationsLimitProvider

import io.tolgee.dtos.UsageLimits
import org.springframework.stereotype.Component

@Component
class BaseLimitsProvider : LimitsProvider {
  override fun getLimits(organizationId: Long): UsageLimits {
    return UsageLimits(
      isPayAsYouGo = false,
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
      strings =
        UsageLimits.Limit(
          included = -1,
          limit = -1,
        ),
      translationSlots =
        UsageLimits.Limit(
          included = -1,
          limit = -1,
        ),
      isTrial = false,
    )
  }
}
