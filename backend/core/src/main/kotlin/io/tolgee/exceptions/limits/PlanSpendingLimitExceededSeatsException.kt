package io.tolgee.exceptions.limits

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class PlanSpendingLimitExceededSeatsException(
  required: Long,
  limit: Long,
) : BadRequestException(
    Message.SEATS_SPENDING_LIMIT_EXCEEDED,
    params = listOf(required, limit),
  )
