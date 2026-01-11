package io.tolgee.exceptions

import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType

class FormalityNotSupportedException(
  val tag: String,
  val service: MtServiceType,
) : BadRequestException(
    Message.FORMALITY_NOT_SUPPORTED_BY_SERVICE,
    params = listOf(tag, service),
  )
