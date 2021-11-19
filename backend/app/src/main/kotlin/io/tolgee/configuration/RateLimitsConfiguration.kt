package io.tolgee.configuration

import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.rateLimis.RateLimit
import io.tolgee.security.rateLimis.RateLimitsManager
import org.springframework.context.annotation.Configuration

@Configuration
class RateLimitsConfiguration(rateLimitsManager: RateLimitsManager) {

  init {
    rateLimitsManager.addRateLimit(
      /**
       * I don't see any use case anybody would need to request
       * more than 20 000 times in 5 minutes from single IP
       */
      RateLimit(
        urlMatcher = Regex("/.*"),
        keyPrefix = "ip",
        keyProvider = { req, _ -> req.remoteAddr },
        bucketSizeProvider = { 20000 },
        timeToRefillInMs = 5 * 60000,
      ),

      /**
       * User rate limit according to pricing
       */
      RateLimit(
        urlMatcher = Regex("/.*"),
        condition = { _, context ->
          val authenticationFacade = context.getBean(AuthenticationFacade::class.java)
          authenticationFacade.userAccountOrNull?.let { true } ?: false
        },
        keyPrefix = "user_id",
        keyProvider = { _, context ->
          val authenticationFacade = context.getBean(AuthenticationFacade::class.java)
          authenticationFacade.userAccount.id.toString()
        },
        bucketSizeProvider = { 400 },
        timeToRefillInMs = 60000,
      ),

      /**
       * Rate limit for login or register from IP
       */
      RateLimit(
        urlMatcher = Regex(
          "/api/public/generatetoken.*" +
            "|/api/public/reset_password.*" +
            "|/api/public/sign_up.*" +
            "|/api/public/authorize_oauth"
        ),
        keyPrefix = "auth_req_ip",
        keyProvider = { req, _ -> req.remoteAddr },
        bucketSizeProvider = { 1000 },
        timeToRefillInMs = 3600000,
      ),
    )
  }
}
