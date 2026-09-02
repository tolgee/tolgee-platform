package io.tolgee.exceptions.limits

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class PlanSpendingLimitExceededWordsException(
  required: Long,
  limit: Long,
) : BadRequestException(
    Message.WORDS_SPENDING_LIMIT_EXCEEDED,
    params = listOf(required, limit),
  )
