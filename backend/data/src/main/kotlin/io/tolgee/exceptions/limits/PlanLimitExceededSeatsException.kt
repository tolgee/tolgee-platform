package io.tolgee.exceptions.limits

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class PlanLimitExceededSeatsException(required: Long, limit: Long) :
  BadRequestException(Message.PLAN_SEAT_LIMIT_EXCEEDED, params = listOf(required, limit))
