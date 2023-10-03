package io.tolgee.exceptions

import org.springframework.http.HttpStatus
import java.io.Serializable

class InternalException : ErrorException {
  constructor(message: io.tolgee.constants.Message, params: List<Serializable>?) : super(message, params)
  constructor(message: io.tolgee.constants.Message) : super(message)

  override val httpStatus: HttpStatus
    get() = HttpStatus.INTERNAL_SERVER_ERROR
}
