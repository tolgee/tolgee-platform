package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus
import java.io.Serializable

abstract class ErrorException : RuntimeException {
  val params: List<Serializable?>?
  val code: String

  constructor(message: Message, params: List<Serializable?>?) : super(message.code) {
    this.params = params
    this.code = message.code
  }

  constructor(message: Message) : this(message, null)

  constructor(code: String, params: List<Serializable>? = null) : super(code + params.toString()) {
    this.code = code
    this.params = params
  }

  val errorResponseBody: ErrorResponseBody
    get() = ErrorResponseBody(this.code, params)
  abstract val httpStatus: HttpStatus?
}
