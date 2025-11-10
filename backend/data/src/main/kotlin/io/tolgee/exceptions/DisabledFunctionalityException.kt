package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class DisabledFunctionalityException(
  message: Message,
) : ErrorException(message) {
  override val httpStatus: HttpStatus
    get() = HttpStatus.CONFLICT
}
