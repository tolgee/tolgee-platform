package io.tolgee.exceptions

import org.springframework.http.HttpStatus

class PermissionException(message: io.tolgee.constants.Message = io.tolgee.constants.Message.OPERATION_NOT_PERMITTED) : ErrorException(message) {
  override val httpStatus: HttpStatus
    get() = HttpStatus.FORBIDDEN
}
