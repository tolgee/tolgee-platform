package io.tolgee.ee.component.limitsAndReporting.generic

import io.tolgee.dtos.UsageLimits
import io.tolgee.exceptions.limits.PlanLimitExceededWordsException
import io.tolgee.exceptions.limits.PlanSpendingLimitExceededWordsException

class WordsLimitChecker(
  limits: UsageLimits,
) : GenericLimitChecker(
    limit = limits.words,
    isPayAsYouGo = limits.isPayAsYouGo,
    includedUsageExceededExceptionProvider = { req ->
      PlanLimitExceededWordsException(required = req, limit = limits.words.limit)
    },
    spendingLimitExceededExceptionProvider = { req ->
      PlanSpendingLimitExceededWordsException(required = req, limit = limits.words.limit)
    },
  )
