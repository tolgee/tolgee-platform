package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus
import java.io.Serializable

class InternalException : ErrorException {
  constructor(message: Message, params: List<Serializable>?) : super(message, params) {}
  constructor(message: Message) : super(message) {}

  override val httpStatus: HttpStatus?
    get() = HttpStatus.INTERNAL_SERVER_ERROR
}
