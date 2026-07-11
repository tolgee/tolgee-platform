package io.tolgee.ee.component.limitsAndReporting.generic

import io.tolgee.dtos.UsageLimits
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.limits.PlanLimitExceededSeatsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededSeatsException

class SeatsLimitChecker(
  limits: UsageLimits,
  includedUsageExceededExceptionProvider: (Long) -> BadRequestException = { req ->
    PlanLimitExceededSeatsException(req, limit = limits.seats.limit)
  },
  spendingLimitExceededExceptionProvider: (Long) -> BadRequestException = { req ->
    PlanSpendingLimitExceededSeatsException(req, limit = limits.seats.limit)
  },
) : GenericLimitChecker(
    limit = limits.seats,
    isPayAsYouGo = limits.isPayAsYouGo,
    includedUsageExceededExceptionProvider = includedUsageExceededExceptionProvider,
    spendingLimitExceededExceptionProvider = spendingLimitExceededExceptionProvider,
  )
