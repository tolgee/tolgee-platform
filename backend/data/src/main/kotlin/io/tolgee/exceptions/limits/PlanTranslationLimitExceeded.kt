package io.tolgee.exceptions.limits

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class PlanTranslationLimitExceeded(required: Long, limit: Long) :
  BadRequestException(Message.PLAN_TRANSLATION_LIMIT_EXCEEDED, params = listOf(required, limit))
