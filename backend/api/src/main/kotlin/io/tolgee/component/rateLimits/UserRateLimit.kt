package io.tolgee.component.rateLimits

import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.rateLimis.RateLimit
import io.tolgee.security.rateLimis.RateLimitLifeCyclePoint
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

/**
 * User rate limit according to pricing
 */
@Component
class UserRateLimit : RateLimit {
  override val urlMatcher: Regex
    get() = Regex("/.*")

  override val condition: (
    request: HttpServletRequest,
    applicationContext: ApplicationContext
  ) -> Boolean = { _, context ->
    val authenticationFacade = context.getBean(AuthenticationFacade::class.java)
    authenticationFacade.userAccountOrNull?.let { true } ?: false
  }

  override val keyPrefix: String = "user_id"

  override val keyProvider: (
    request: HttpServletRequest,
    applicationContext: ApplicationContext
  ) -> String = { _, context ->
    val authenticationFacade = context.getBean(AuthenticationFacade::class.java)
    authenticationFacade.userAccount.id.toString()
  }

  override val bucketSizeProvider = { 400 }

  override val timeToRefillInMs = 60000

  override val lifeCyclePoint: RateLimitLifeCyclePoint = RateLimitLifeCyclePoint.AFTER_AUTHORIZATION
}
