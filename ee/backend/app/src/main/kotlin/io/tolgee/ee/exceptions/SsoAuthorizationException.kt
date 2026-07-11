package io.tolgee.ee.exceptions

import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import java.io.Serializable

class SsoAuthorizationException : AuthenticationException {
  constructor(message: Message) : super(message)

  constructor(message: Message, params: List<Serializable?>? = null, cause: Exception? = null) : super(
    message,
    params,
    cause,
  )

  constructor(code: String, params: List<Serializable?>? = null, cause: Exception? = null) : super(code, params, cause)
}
