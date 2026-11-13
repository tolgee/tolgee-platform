package io.tolgee.exceptions

import io.tolgee.constants.Message
import io.tolgee.constants.MessageInterface
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.NOT_FOUND)
open class NotFoundException(
  val msg: MessageInterface = Message.RESOURCE_NOT_FOUND,
  val resourceId: Any? = null,
) : RuntimeException(),
  ExpectedException,
  ExceptionWithCode {
  override val code: String
    get() = msg.code
}
