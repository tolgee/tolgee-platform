package io.tolgee.exceptions

import org.springframework.http.HttpStatus

class PermissionException : ErrorException(io.tolgee.constants.Message.OPERATION_NOT_PERMITTED) {
  override val httpStatus: HttpStatus
    get() = HttpStatus.FORBIDDEN
}
