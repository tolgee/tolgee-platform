package io.tolgee.security.rateLimits

import javax.servlet.http.HttpServletRequest

class RateLimit(
  val urlMatcher: Regex,
  val keyPrefix: String,
  val keyProvider: (request: HttpServletRequest) -> String,
  val bucketSizeProvider: () -> Int,
  val timeToRefillInMs: Int,
)
