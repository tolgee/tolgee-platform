package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus

class BranchWriteLockedException(
  message: Message,
) : ErrorException(message) {
  override val httpStatus: HttpStatus = HttpStatus.LOCKED
}
