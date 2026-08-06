package io.tolgee.ee.component.limitsAndReporting.generic

import io.tolgee.dtos.UsageLimits
import io.tolgee.exceptions.limits.PlanLimitExceededWordsException

class WordsLimitChecker(
  limits: UsageLimits,
) : GenericLimitChecker(
    limit = limits.words,
    isPayAsYouGo = false,
    includedUsageExceededExceptionProvider = { req ->
      PlanLimitExceededWordsException(required = req, limit = limits.words.limit)
    },
    spendingLimitExceededExceptionProvider = { req ->
      PlanLimitExceededWordsException(required = req, limit = limits.words.limit)
    },
  )
