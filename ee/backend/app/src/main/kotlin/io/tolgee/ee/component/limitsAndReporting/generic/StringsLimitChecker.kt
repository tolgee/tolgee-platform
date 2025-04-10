package io.tolgee.ee.component.limitsAndReporting.generic

import io.tolgee.dtos.UsageLimits
import io.tolgee.exceptions.limits.PlanLimitExceededStringsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededStringsException

class StringsLimitChecker(
  private val required: Long?,
  private val limits: UsageLimits,
) {
  fun check() {
    required ?: return

    GenericLimitChecker(
      required,
      limit = limits.strings,
      isPayAsYouGo = limits.isPayAsYouGo,
      includedUsageExceededExceptionProvider = {
        PlanLimitExceededStringsException(
          required = required,
          limit = limits.strings.limit,
        )
      },
      spendingLimitExceededExceptionProvider = {
        PlanSpendingLimitExceededStringsException(required = required, limit = limits.strings.limit)
      },
    ).checkLimit()
  }
}
