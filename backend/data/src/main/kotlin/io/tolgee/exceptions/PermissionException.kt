package io.tolgee.exceptions

import io.tolgee.constants.Message
import io.tolgee.constants.MessageInterface
import io.tolgee.model.enums.Scope
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.io.Serializable

@ResponseStatus(HttpStatus.FORBIDDEN)
open class PermissionException(
  message: MessageInterface = Message.OPERATION_NOT_PERMITTED,
  params: List<Serializable?>? = null,
) : ErrorException(message, params) {
  constructor(missingScopes: List<Scope>) : this(
    params = missingScopes.map { it.value },
  )

  override val httpStatus: HttpStatus
    get() = HttpStatus.FORBIDDEN
}
