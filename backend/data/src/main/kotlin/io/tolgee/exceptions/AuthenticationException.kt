package io.tolgee.exceptions

import org.springframework.http.HttpStatus

class AuthenticationException(message: io.tolgee.constants.Message) : ErrorException(message) {

  override val httpStatus: HttpStatus
    get() = HttpStatus.UNAUTHORIZED
}
