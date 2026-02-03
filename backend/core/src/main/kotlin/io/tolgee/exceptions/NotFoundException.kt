package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.NOT_FOUND)
open class NotFoundException(
  val msg: Message = Message.RESOURCE_NOT_FOUND,
  val resourceId: Any? = null,
) : RuntimeException(),
  ExpectedException
