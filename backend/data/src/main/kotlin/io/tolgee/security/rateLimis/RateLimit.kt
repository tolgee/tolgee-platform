package io.tolgee.security.rateLimis

import org.springframework.context.ApplicationContext
import javax.servlet.http.HttpServletRequest

interface RateLimit {
  val urlMatcher: Regex
  val keyPrefix: String
  val condition: (request: HttpServletRequest, applicationContext: ApplicationContext) -> Boolean
    get() = { _, _ -> true }
  val keyProvider: (request: HttpServletRequest, applicationContext: ApplicationContext) -> String
  val bucketSizeProvider: () -> Int
  val timeToRefillInMs: Int
  val lifeCyclePoint: RateLimitLifeCyclePoint
    get() = RateLimitLifeCyclePoint.ENTRY
}
