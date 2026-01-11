package io.tolgee.component.bucket

import java.time.Duration

class TokenBucket(
  currentTimestamp: Long,
  var size: Long,
  var tokens: Long,
  var period: Duration,
) {
  var refillAt: Long

  init {
    refillAt = currentTimestamp + period.toMillis()
  }

  fun refillIfItsTime(
    currentTimestamp: Long,
    newTokens: Long,
    renewPeriod: Duration,
  ): TokenBucket {
    if (isTimeToRefill(currentTimestamp)) {
      this.tokens = newTokens
      this.size = newTokens
      this.refillAt = currentTimestamp + renewPeriod.toMillis()
      this.period = renewPeriod
    }
    return this
  }

  fun isTimeToRefill(currentTimestamp: Long) = refillAt <= currentTimestamp
}
