package io.tolgee.ee.component.limitsAndReporting.generic

import io.tolgee.dtos.UsageLimits
import io.tolgee.exceptions.limits.PlanLimitExceededStringsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededStringsException

class StringsLimitChecker(
  limits: UsageLimits,
) : GenericLimitChecker(
    limit = limits.strings,
    isPayAsYouGo = limits.isPayAsYouGo,
    includedUsageExceededExceptionProvider = { req ->
      PlanLimitExceededStringsException(required = req, limit = limits.strings.limit)
    },
    spendingLimitExceededExceptionProvider = { req ->
      PlanSpendingLimitExceededStringsException(required = req, limit = limits.strings.limit)
    },
  )
