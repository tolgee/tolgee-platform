package io.tolgee.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class AuthenticationException(message: io.tolgee.constants.Message) : ErrorException(message) {
  override val httpStatus: HttpStatus
    get() = HttpStatus.UNAUTHORIZED
}
