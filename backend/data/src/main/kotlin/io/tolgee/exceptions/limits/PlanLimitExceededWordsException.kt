package io.tolgee.exceptions.limits

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class PlanLimitExceededWordsException(
  required: Long,
  limit: Long,
) : BadRequestException(Message.PLAN_WORD_LIMIT_EXCEEDED, params = listOf(required, limit))
