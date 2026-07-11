package io.tolgee.ee.component.limitsAndReporting.generic

import io.tolgee.dtos.UsageLimits
import io.tolgee.exceptions.limits.PlanLimitExceededKeysException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededKeysException

class KeysLimitChecker(
  limits: UsageLimits,
) : GenericLimitChecker(
    limit = limits.keys,
    isPayAsYouGo = limits.isPayAsYouGo,
    includedUsageExceededExceptionProvider = { req ->
      PlanLimitExceededKeysException(required = req, limit = limits.keys.limit)
    },
    spendingLimitExceededExceptionProvider = { req ->
      PlanSpendingLimitExceededKeysException(required = req, limit = limits.keys.limit)
    },
  )
