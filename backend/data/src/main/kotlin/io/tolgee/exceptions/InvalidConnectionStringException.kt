package io.tolgee.exceptions

import io.tolgee.constants.Message

class InvalidConnectionStringException(
  cause: Exception? = null,
) : BadRequestException(Message.INVALID_CONNECTION_STRING, cause)
