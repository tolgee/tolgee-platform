package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus
import java.io.Serializable

open class FailedDependencyException : ErrorException {
  constructor(message: Message, params: List<Serializable?>?, cause: Exception? = null) : super(
    message,
    params,
    cause,
  )

  constructor(message: Message, cause: Exception? = null) : super(message, cause = cause)
  constructor(code: String, params: List<Serializable?>? = null, cause: Exception? = null) : super(
    code,
    params,
    cause,
  )

  override val httpStatus: HttpStatus
    get() = HttpStatus.FAILED_DEPENDENCY
}
