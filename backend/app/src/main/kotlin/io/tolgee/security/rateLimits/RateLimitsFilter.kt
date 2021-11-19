package io.tolgee.security

import io.tolgee.component.lockingProvider.LockingProvider
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.security.rateLimits.RateLimitsManager
import io.tolgee.security.rateLimits.UsageEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class RateLimitsFilter(
  private val cacheManager: CacheManager,
  private val lockingProvider: LockingProvider,
  private val rateLimitsManager: RateLimitsManager,
  @param:Qualifier("handlerExceptionResolver")
  private val resolver: HandlerExceptionResolver
) : OncePerRequestFilter() {
  val logger: Logger = LoggerFactory.getLogger(RateLimitsFilter::class.java)

  override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
    try {
      rateLimitsManager.rateLimits.forEach { rateLimit ->
        val cache = cacheManager.getCache(Caches.RATE_LIMITS) ?: throw Exception("No such cache")

        if (request.requestURI.matches(rateLimit.urlMatcher)) {
          val key = "${rateLimit.keyPrefix}_${rateLimit.keyProvider(request)}"

          val lock = lockingProvider.getLock(key)
          lock.lock()
          try {
            var current = (cache.get(key)?.get() as? String)?.let {
              UsageEntry.deserialize(it)
            }

            val bucketSize = rateLimit.bucketSizeProvider()

            // first request, create new UsageEntry
            if (current == null) {
              current = UsageEntry(Date(), bucketSize)
            } else if (Date().time - current.time.time > rateLimit.timeToRefillInMs) {
              current = UsageEntry(Date(), bucketSize)
            }

            if (current.availableTokens < 1) {
              throw BadRequestException(
                Message.TOO_MANY_REQUESTS,
                listOf(
                  mapOf(
                    "bucketSize" to bucketSize,
                    "timeToRefill" to rateLimit.timeToRefillInMs
                  ).toMap(HashMap())
                )
              )
            }

            current.availableTokens -= 1

            logger.info(
              "Accesses: " + key + " - " +
                (Date().time - current.time.time).toDouble() / 1000 +
                "s unitil refill, tokens: " +
                current.availableTokens
            )

            cache.put(key, current.serialize())
          } finally {
            lock.unlock()
          }
        }
      }

      filterChain.doFilter(request, response)
    } catch (e: Exception) {
      resolver.resolveException(request, response, null, e)
    }
  }
}
