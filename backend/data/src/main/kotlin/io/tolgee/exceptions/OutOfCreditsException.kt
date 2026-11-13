package io.tolgee.exceptions

import io.tolgee.constants.Message

class OutOfCreditsException(
  val reason: Reason,
  cause: Throwable? = null,
) : RuntimeException(cause) {
  enum class Reason(
    val tolgeeMessage: Message,
  ) {
    OUT_OF_CREDITS(Message.OUT_OF_CREDITS),
    SPENDING_LIMIT_EXCEEDED(Message.CREDIT_SPENDING_LIMIT_EXCEEDED),
  }
}
