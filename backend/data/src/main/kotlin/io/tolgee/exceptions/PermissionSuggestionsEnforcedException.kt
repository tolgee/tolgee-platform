package io.tolgee.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.io.Serializable

@ResponseStatus(HttpStatus.FORBIDDEN)
open class PermissionSuggestionsEnforcedException(
  message: io.tolgee.constants.Message = io.tolgee.constants.Message.OPERATION_NOT_PERMITTED,
  params: List<Serializable?>? = null,
): PermissionException(message, params)