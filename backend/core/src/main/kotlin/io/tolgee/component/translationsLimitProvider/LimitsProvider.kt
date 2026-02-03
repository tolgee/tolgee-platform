package io.tolgee.component.translationsLimitProvider

import io.tolgee.dtos.UsageLimits

interface LimitsProvider {
  fun getLimits(organizationId: Long): UsageLimits
}
