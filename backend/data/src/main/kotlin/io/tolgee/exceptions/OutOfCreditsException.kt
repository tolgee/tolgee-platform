package io.tolgee.exceptions

class OutOfCreditsException(val reason: Reason) : RuntimeException() {
  enum class Reason {
    OUT_OF_CREDITS,
    SPENDING_LIMIT_EXCEEDED,
  }
}
