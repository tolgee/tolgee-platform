package io.tolgee.security.rateLimits

import io.tolgee.component.lockingProvider.LockingProvider
import io.tolgee.security.RateLimitsFilter
import io.tolgee.security.rateLimis.RateLimitLifeCyclePoint
import io.tolgee.security.rateLimis.RateLimitsManager
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class RateLimitsFilterFactory(
  private val cacheManager: CacheManager,
  private val lockingProvider: LockingProvider,
  private val rateLimitsManager: RateLimitsManager,
  private val applicationContext: ApplicationContext,
  private val rateLimitParamsProxy: RateLimitParamsProxy,
  @param:Qualifier("handlerExceptionResolver")
  private val resolver: HandlerExceptionResolver,
) {
  fun create(rateLimitLifeCyclePoint: RateLimitLifeCyclePoint): RateLimitsFilter {
    return RateLimitsFilter(
      cacheManager = cacheManager,
      lockingProvider = lockingProvider,
      rateLimitsManager = rateLimitsManager,
      resolver = resolver,
      applicationContext = applicationContext,
      rateLimitsParamsProxy = rateLimitParamsProxy,
      lifeCyclePoint = rateLimitLifeCyclePoint
    )
  }
}
