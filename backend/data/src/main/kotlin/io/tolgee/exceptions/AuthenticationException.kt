package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.io.Serializable

@ResponseStatus(HttpStatus.UNAUTHORIZED)
open class AuthenticationException(
  message: Message,
  params: List<Serializable?>? = null,
) : ErrorException(message, params) {
  override val httpStatus: HttpStatus
    get() = HttpStatus.UNAUTHORIZED
}
