package io.tolgee.exceptions

import org.springframework.http.HttpStatus
import java.io.Serializable

class TooManyRequestsException : ErrorException {
  constructor(message: io.tolgee.constants.Message, params: List<Serializable?>?, cause: Exception? = null) : super(
    message,
    params,
    cause,
  )

  constructor(message: io.tolgee.constants.Message, cause: Exception? = null) : super(message, cause = cause)
  constructor(code: String, params: List<Serializable?>? = null, cause: Exception? = null) : super(
    code,
    params,
    cause,
  )

  override val httpStatus: HttpStatus
    get() = HttpStatus.TOO_MANY_REQUESTS
}
