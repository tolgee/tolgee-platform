package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus
import java.io.Serializable

abstract class ErrorException :
  ExceptionWithMessage,
  ExpectedException {
  constructor(message: Message, params: List<Serializable?>? = null, cause: Exception? = null) : super(
    message,
    params,
    cause,
  )

  constructor(code: String, params: List<Serializable?>? = null, cause: Exception? = null) : super(code, params, cause)

  val errorResponseBody: ErrorResponseBody
    get() = ErrorResponseBody(this.code, params)

  abstract val httpStatus: HttpStatus
}
