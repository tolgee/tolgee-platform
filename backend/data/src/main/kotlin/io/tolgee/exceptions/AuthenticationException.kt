package io.tolgee.exceptions

import io.tolgee.constants.MessageInterface
import org.springframework.http.HttpStatus
import java.io.Serializable

open class AuthenticationException : ErrorException {
  constructor(message: MessageInterface) : super(message)

  constructor(message: MessageInterface, params: List<Serializable?>? = null, cause: Exception? = null) : super(
    message,
    params,
    cause,
  )

  constructor(code: String, params: List<Serializable?>? = null, cause: Exception? = null) : super(code, params, cause)

  override val httpStatus: HttpStatus
    get() = HttpStatus.UNAUTHORIZED
}
