package io.tolgee.exceptions.limits

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class PlanKeysLimitExceeded(required: Long, limit: Long) :
  BadRequestException(Message.PLAN_KEY_LIMIT_EXCEEDED, params = listOf(required, limit))
