package io.tolgee.security.rateLimits

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class RateLimitsManager {
  private val _rateLimits = mutableListOf<RateLimit>(
    /**
     * I don't see any use case anybody would need to request
     * more than 20 000 times in 5 minutes from single IP
     */
    RateLimit(
      urlMatcher = Regex("/.*"),
      keyPrefix = "ip",
      keyProvider = { it.remoteAddr },
      bucketSizeProvider = { 20000 },
      timeToRefillInMs = 5 * 60000,
    )
  )

  fun addRateLimit(rateLimit: RateLimit) {
    _rateLimits.add(rateLimit)
  }

  val rateLimits: List<RateLimit>
    get() = _rateLimits.toList()
}
