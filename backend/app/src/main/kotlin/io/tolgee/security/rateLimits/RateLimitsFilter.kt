package io.tolgee.security.rateLimits

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.lockingProvider.LockingProvider
import io.tolgee.configuration.tolgee.RateLimitProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.security.rateLimis.RateLimit
import io.tolgee.security.rateLimis.RateLimitLifeCyclePoint
import io.tolgee.security.rateLimis.UsageEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.context.ApplicationContext
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import java.util.concurrent.locks.Lock
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RateLimitsFilter(
  private val cacheManager: CacheManager,
  private val lockingProvider: LockingProvider,
  @param:Qualifier("handlerExceptionResolver")
  private val resolver: HandlerExceptionResolver,
  private val applicationContext: ApplicationContext,
  private val rateLimitsParamsProxy: RateLimitParamsProxy,
  private val lifeCyclePoint: RateLimitLifeCyclePoint,
  private val rateLimits: List<RateLimit>,
  private val rateLimitProperties: RateLimitProperties,
  private val currentDateProvider: CurrentDateProvider
) : OncePerRequestFilter() {

  init {
    setBeanName("RateLimitsFilter${lifeCyclePoint.name}")
  }

  val logger: Logger = LoggerFactory.getLogger(RateLimitsFilter::class.java)

  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    if (!rateLimitProperties.enabled) {
      filterChain.doFilter(request, response)
      return
    }

    try {
      getApplicableRateLimits(request).forEach { rateLimit ->
        if (isRequestUrlMatching(request, rateLimit)) {
          val key = "${rateLimit.keyPrefix}_${rateLimit.keyProvider(request, applicationContext)}"

          val lock = lockKeyResource(key)
          try {
            var current = getCurrentUsageEntry(key)

            val bucketSize = rateLimitsParamsProxy.getBucketSize(rateLimit.keyPrefix, rateLimit.bucketSizeProvider())
            val timeToRefill = rateLimitsParamsProxy.getTimeToRefill(rateLimit.keyPrefix, rateLimit.timeToRefillInMs)

            // first request, create new UsageEntry
            val time = currentDateProvider.getDate()
            if (current == null) {
              current = UsageEntry(time, bucketSize)
            } else if (time.time - current.time.time > timeToRefill) {
              current = UsageEntry(time, bucketSize)
            }

            throwWhenOutOfTokens(current, bucketSize, timeToRefill, rateLimit)

            current.availableTokens -= 1

            logDebugInfo(key, current)

            cache.put(key, current.serialize())
          } finally {
            lock.unlock()
          }
        }
      }
    } catch (e: Exception) {
      resolver.resolveException(request, response, null, e)
    }
    filterChain.doFilter(request, response)
  }

  private fun logDebugInfo(key: String, current: UsageEntry) {
    val time = currentDateProvider.getDate()
    logger.debug(
      "Accesses: " + key + " - " +
        (time.time - current.time.time).toDouble() / 1000 +
        "s unitil refill, tokens: " +
        current.availableTokens
    )
  }

  private fun throwWhenOutOfTokens(
    current: UsageEntry,
    bucketSize: Int,
    timeToRefill: Int,
    rateLimit: RateLimit
  ) {
    if (current.availableTokens < 1) {
      throw BadRequestException(
        Message.TOO_MANY_REQUESTS,
        listOf(
          mapOf(
            "bucketSize" to bucketSize,
            "timeToRefill" to timeToRefill,
            "keyPrefix" to rateLimit.keyPrefix
          ).toMap(HashMap())
        )
      )
    }
  }

  private fun getCurrentUsageEntry(key: String): UsageEntry? {
    return (cache.get(key)?.get() as? String)?.let {
      UsageEntry.deserialize(it)
    }
  }

  private fun lockKeyResource(key: String): Lock {
    val lock = lockingProvider.getLock(key)
    lock.lock()
    return lock
  }

  private val cache by lazy {
    return@lazy cacheManager.getCache(Caches.RATE_LIMITS) ?: throw Exception("No such cache")
  }

  private fun isRequestUrlMatching(
    request: HttpServletRequest,
    rateLimit: RateLimit
  ) = request.requestURI.matches(rateLimit.urlMatcher)

  private fun getApplicableRateLimits(request: HttpServletRequest) = rateLimits.asSequence()
    .filter { it.lifeCyclePoint == lifeCyclePoint && it.condition(request, applicationContext) }
}
