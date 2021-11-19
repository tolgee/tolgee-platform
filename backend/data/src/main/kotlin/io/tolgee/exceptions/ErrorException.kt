package io.tolgee.exceptions

import org.springframework.http.HttpStatus
import java.io.Serializable

abstract class ErrorException : RuntimeException {
  val params: List<Serializable>?
  val code: String

  constructor(message: io.tolgee.constants.Message, params: List<Serializable>?) : super(message.code) {
    this.params = params
    this.code = message.code
  }

  constructor(message: io.tolgee.constants.Message) : this(message, null)

  constructor(code: String, params: List<Serializable>? = null) {
    this.code = code
    this.params = params
  }

  val errorResponseBody: ErrorResponseBody
    get() = ErrorResponseBody(this.code, params)
  abstract val httpStatus: HttpStatus?
}
