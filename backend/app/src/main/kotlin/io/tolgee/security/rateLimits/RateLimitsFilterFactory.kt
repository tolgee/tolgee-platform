package io.tolgee.security.rateLimits

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.lockingProvider.LockingProvider
import io.tolgee.configuration.tolgee.RateLimitProperties
import io.tolgee.security.rateLimis.RateLimit
import io.tolgee.security.rateLimis.RateLimitLifeCyclePoint
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class RateLimitsFilterFactory(
  private val cacheManager: CacheManager,
  private val lockingProvider: LockingProvider,
  private val applicationContext: ApplicationContext,
  private val rateLimitParamsProxy: RateLimitParamsProxy,
  @param:Qualifier("handlerExceptionResolver")
  private val resolver: HandlerExceptionResolver,
  private val rateLimits: List<RateLimit>,
  private val currentDateProvider: CurrentDateProvider,
  private val rateLimitProperties: RateLimitProperties
) {
  fun create(rateLimitLifeCyclePoint: RateLimitLifeCyclePoint): RateLimitsFilter {
    return RateLimitsFilter(
      cacheManager = cacheManager,
      lockingProvider = lockingProvider,
      resolver = resolver,
      applicationContext = applicationContext,
      rateLimitsParamsProxy = rateLimitParamsProxy,
      lifeCyclePoint = rateLimitLifeCyclePoint,
      rateLimits = rateLimits,
      currentDateProvider = currentDateProvider,
      rateLimitProperties = rateLimitProperties
    )
  }
}
