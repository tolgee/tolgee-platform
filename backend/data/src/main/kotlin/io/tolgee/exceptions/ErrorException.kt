package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus
import java.io.Serializable

abstract class ErrorException : ExceptionWithMessage {
  constructor(message: Message, params: List<Serializable?>?) : super(message.code, params)

  constructor(message: Message) : this(message, null)

  constructor(code: String, params: List<Serializable?>? = null) : super(code, params)

  val errorResponseBody: ErrorResponseBody
    get() = ErrorResponseBody(this.code, params)

  abstract val httpStatus: HttpStatus?
}
