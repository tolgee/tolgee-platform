package io.tolgee.exceptions

import org.springframework.http.HttpStatus
import java.io.Serializable

open class PermissionException(
  message: io.tolgee.constants.Message = io.tolgee.constants.Message.OPERATION_NOT_PERMITTED,
  params: List<Serializable?>? = null
) : ErrorException(message, params) {
  override val httpStatus: HttpStatus?
    get() = HttpStatus.FORBIDDEN
}
