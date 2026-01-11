package io.tolgee.exceptions

import io.tolgee.constants.Message

class AuthExpiredException(
  message: Message,
) : AuthenticationException(message)
