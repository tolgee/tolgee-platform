package io.tolgee.security.rateLimis

import org.springframework.context.ApplicationContext
import javax.servlet.http.HttpServletRequest

class RateLimit(
  val urlMatcher: Regex,
  val keyPrefix: String,
  val condition: (request: HttpServletRequest, applicationContext: ApplicationContext) -> Boolean = { _, _ -> true },
  val keyProvider: (request: HttpServletRequest, applicationContext: ApplicationContext) -> String,
  val bucketSizeProvider: () -> Int,
  val timeToRefillInMs: Int,
  val lifeCyclePoint: RateLimitLifeCyclePoint = RateLimitLifeCyclePoint.ENTRY
)
