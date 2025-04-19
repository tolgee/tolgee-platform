package io.tolgee.ee.component.limitsAndReporting.generic

import io.tolgee.dtos.UsageLimits
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.limits.PlanLimitExceededSeatsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededSeatsException

open class SeatsLimitChecker(
  private val required: Long?,
  private val limits: UsageLimits,
) {
  fun check() {
    required ?: return

    GenericLimitChecker(
      required = required,
      limit = limits.seats,
      isPayAsYouGo = limits.isPayAsYouGo,
      includedUsageExceededExceptionProvider = {
        getIncludedUsageExceededException()
      },
      spendingLimitExceededExceptionProvider = {
        getSpendingLimitExceededException()
      },
    ).checkLimit()
  }

  open fun getIncludedUsageExceededException(): BadRequestException {
    return PlanLimitExceededSeatsException(required!!, limit = limits.seats.limit)
  }

  open fun getSpendingLimitExceededException(): BadRequestException {
    return PlanSpendingLimitExceededSeatsException(required!!, limit = limits.seats.limit)
  }
}
