package io.tolgee.exceptions

class OutOfCreditsException(
  val reason: Reason,
  cause: Throwable? = null,
) : RuntimeException(cause) {
  enum class Reason {
    OUT_OF_CREDITS,
    SPENDING_LIMIT_EXCEEDED,
  }
}
