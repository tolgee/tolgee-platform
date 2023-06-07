package io.tolgee.component.rateLimits

import io.tolgee.security.rateLimis.RateLimit
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

/**
 * I don't see any use case anybody would need to request
 * more than 20 000 times in 5 minutes from single IP
 */
@Component
class BaseIpRateLimit : RateLimit {
  override val urlMatcher = Regex("/.*")
  override val keyPrefix = "ip"
  override val keyProvider: (
    request: HttpServletRequest,
    applicationContext: ApplicationContext
  ) -> String = { req, _ -> req.remoteAddr }
  override val bucketSizeProvider: () -> Int = { 20000 }
  override val timeToRefillInMs = 5 * 60000
}
