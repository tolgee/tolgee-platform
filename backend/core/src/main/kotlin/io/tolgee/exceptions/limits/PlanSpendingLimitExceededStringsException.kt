package io.tolgee.exceptions.limits

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class PlanSpendingLimitExceededStringsException(
  required: Long,
  limit: Long,
) : BadRequestException(
    Message.TRANSLATION_SPENDING_LIMIT_EXCEEDED,
    params = listOf(required, limit),
  )
