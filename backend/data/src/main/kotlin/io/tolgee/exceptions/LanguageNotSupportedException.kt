package io.tolgee.exceptions

import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType

class LanguageNotSupportedException(
  val tag: String,
  val service: MtServiceType,
) : BadRequestException(
    Message.LANGUAGE_NOT_SUPPORTED_BY_SERVICE,
    params = listOf(tag, service),
  )
