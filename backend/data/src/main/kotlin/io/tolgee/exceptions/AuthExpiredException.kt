package io.tolgee.exceptions

import io.tolgee.constants.MessageInterface

class AuthExpiredException(
  message: MessageInterface,
) : AuthenticationException(message)
