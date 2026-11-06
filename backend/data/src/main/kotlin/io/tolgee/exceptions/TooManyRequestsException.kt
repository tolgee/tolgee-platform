package io.tolgee.exceptions

import io.tolgee.constants.MessageInterface
import org.springframework.http.HttpStatus
import java.io.Serializable

class TooManyRequestsException : ErrorException {
  constructor(message: MessageInterface, params: List<Serializable?>?, cause: Exception? = null) : super(
    message,
    params,
    cause,
  )

  constructor(message: MessageInterface, cause: Exception? = null) : super(message, cause = cause)
  constructor(code: String, params: List<Serializable?>? = null, cause: Exception? = null) : super(
    code,
    params,
    cause,
  )

  override val httpStatus: HttpStatus
    get() = HttpStatus.TOO_MANY_REQUESTS
}
