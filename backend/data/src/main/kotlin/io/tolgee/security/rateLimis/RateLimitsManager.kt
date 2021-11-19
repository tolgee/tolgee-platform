package io.tolgee.security.rateLimis

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class RateLimitsManager {
  private val _rateLimits = mutableListOf<RateLimit>()

  fun addRateLimit(vararg rateLimits: RateLimit) {
    _rateLimits.addAll(rateLimits)
  }

  val rateLimits: List<RateLimit>
    get() = _rateLimits.toList()
}
