package io.tolgee.exceptions.limits

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class PlanSpendingLimitExceededKeysException(
  required: Long,
  limit: Long,
) : BadRequestException(
    Message.KEYS_SPENDING_LIMIT_EXCEEDED,
    params = listOf(required, limit),
  )
