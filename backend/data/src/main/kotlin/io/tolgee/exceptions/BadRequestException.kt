package io.tolgee.exceptions

import org.springframework.http.HttpStatus
import java.io.Serializable

open class BadRequestException : ErrorException {
  constructor(message: io.tolgee.constants.Message, params: List<Serializable>?) : super(message, params)
  constructor(message: io.tolgee.constants.Message) : super(message)
  constructor(code: String, params: List<Serializable>? = null) : super(code, params)

  override val httpStatus: HttpStatus?
    get() = HttpStatus.BAD_REQUEST
}
