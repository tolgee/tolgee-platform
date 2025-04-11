package io.tolgee.ee.component.limitsAndReporting.generic

import io.tolgee.dtos.UsageLimits
import io.tolgee.exceptions.limits.PlanLimitExceededKeysException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededKeysException

class KeysLimitChecker(
  private val required: Long?,
  private val limits: UsageLimits,
) {
  fun check() {
    required ?: return

    GenericLimitChecker(
      required,
      limit = limits.keys,
      isPayAsYouGo = limits.isPayAsYouGo,
      includedUsageExceededExceptionProvider = {
        PlanLimitExceededKeysException(
          required = required,
          limit = limits.keys.limit,
        )
      },
      spendingLimitExceededExceptionProvider = {
        PlanSpendingLimitExceededKeysException(required = required, limit = limits.keys.limit)
      },
    ).checkLimit()
  }
}
