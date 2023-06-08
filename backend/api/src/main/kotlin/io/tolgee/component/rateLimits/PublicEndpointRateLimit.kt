package io.tolgee.component.rateLimits

import io.tolgee.security.rateLimis.RateLimit
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

/**
 * Rate limit for login or register from IP
 */
@Component
class PublicEndpointRateLimit : RateLimit {
  override val urlMatcher: Regex = Regex(
    "/api/public/generatetoken.*" +
      "|/api/public/reset_password.*" +
      "|/api/public/sign_up.*" +
      "|/api/public/authorize_oauth"
  )

  override val keyPrefix: String = "auth_req_ip"

  override val keyProvider: (
    request: HttpServletRequest,
    applicationContext: ApplicationContext
  ) -> String = { req, _ -> req.remoteAddr }

  override val bucketSizeProvider = { 1000 }

  override val timeToRefillInMs = 3600000
}
