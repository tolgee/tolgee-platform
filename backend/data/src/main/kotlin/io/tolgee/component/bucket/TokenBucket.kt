package io.tolgee.component.bucket

import java.time.Duration

data class TokenBucket(
  var refillAt: Long,
  var size: Long,
  var tokens: Long,
  val period: Duration
) {
  fun refillIfItsTime(currentTimestamp: Long, newTokens: Long): TokenBucket {
    if (refillAt < currentTimestamp) {
      this.tokens = newTokens
      this.refillAt = currentTimestamp + period.toMillis()
    }
    return this
  }
}
