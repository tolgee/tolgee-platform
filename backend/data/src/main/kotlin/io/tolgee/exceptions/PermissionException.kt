package io.tolgee.exceptions

import io.tolgee.model.enums.Scope
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.io.Serializable

@ResponseStatus(HttpStatus.FORBIDDEN)
open class PermissionException(
  message: io.tolgee.constants.Message = io.tolgee.constants.Message.OPERATION_NOT_PERMITTED,
  params: List<Serializable?>? = null,
) : ErrorException(message, params) {
  constructor(missingScopes: List<Scope>) : this(
    params = missingScopes.map { it.value },
  )

  override val httpStatus: HttpStatus
    get() = HttpStatus.FORBIDDEN
}
