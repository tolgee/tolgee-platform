package io.tolgee.exceptions

import io.tolgee.constants.Message

class TranslationSpendingLimitExceeded(required: Long, limit: Long) :
  BadRequestException(
    Message.TRANSLATION_SPENDING_LIMIT_EXCEEDED,
    params = listOf(required, limit),
  )
