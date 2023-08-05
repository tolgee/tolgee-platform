/**
 * Copyright (C) 2023 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.ratelimit

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.constants.Caches
import io.tolgee.model.UserAccount
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Service
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

@Service
class RateLimitService(
  private val cacheManager: CacheManager,
  private val lockingProvider: LockingProvider,
  private val currentDateProvider: CurrentDateProvider,
) {

  private val cache: Cache by lazy { cacheManager.getCache(Caches.RATE_LIMITS) }

  /**
   * Consumes a token from the provided rate limit policy.
   *
   * @param policy The rate limit policy.
   * @throws RateLimitedException There are no longer any tokens in the bucket.
   */
  fun consumeBucket(policy: RateLimitPolicy) {
    lockingProvider.withLocking("tolgee.ratelimit.${policy.bucketName}") {
      val time = currentDateProvider.date.time
      val bucket = cache.get(policy.bucketName, Bucket::class.java)

      // No bucket exists for current window
      if (bucket == null || bucket.resetAt < time) {
        val tokensRemaining = policy.limit - 1
        val tokensResetAt = time + policy.windowSize

        cache.put(policy.bucketName, Bucket(tokensRemaining, tokensResetAt))
        return@withLocking
      }

      // Rate limit reached
      if (bucket.tokens == 0) {
        throw RateLimitedException(bucket.resetAt - time)
      }

      // Remove a token
      cache.put(policy.bucketName, Bucket(bucket.tokens - 1, bucket.resetAt))
    }
  }

  /**
   * Returns the global per-ip rate limit policy applicable to the request.
   *
   * @param request The HTTP request.
   * @return The applicable rate limit policy, if any.
   */
  fun getGlobalIpRateLimitPolicy(request: HttpServletRequest): RateLimitPolicy? {
    // TODO: take into account server configuration - DO NOT MERGE UNTIL ADDRESSED.
    return RateLimitPolicy(
      "global.ip.${request.remoteAddr}",
      20_000,
      5 * 60_000
    )
  }

  /**
   * Returns the global per-user rate limit policy applicable to the request.
   *
   * @param request The HTTP request.
   * @param account The authenticated account.
   * @return The applicable rate limit policy, if any.
   */
  fun getGlobalUserRateLimitPolicy(request: HttpServletRequest, account: UserAccount): RateLimitPolicy? {
    // TODO: take into account server configuration - DO NOT MERGE UNTIL ADDRESSED.
    return RateLimitPolicy(
      "global.user.${account.id}",
      400,
      60_000,
    )
  }

  /**
   * Returns the applicable rate limit policy for the HTTP request.
   * This method must be called AFTER the DispatcherServlet processed the request.
   * It is therefore **not suitable for use in a filter**. Use an interceptor instead.
   *
   * @param request The HTTP request.
   * @param account The authenticated account, if authenticated.
   * @return The applicable rate limit policy, if any.
   */
  fun getEndpointRateLimit(request: HttpServletRequest, account: UserAccount?, handler: HandlerMethod): RateLimitPolicy? {
    // TODO: take into account server configuration - DO NOT MERGE UNTIL ADDRESSED.
    val annotation = AnnotationUtils.getAnnotation(handler.method, RateLimited::class.java)
      ?: return null

    val matchedPath = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
    val pathVariablesMap = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>

    val id = if (account?.id != null) "user.${account.id}" else "ip.${request.remoteAddr}"
    val pathVariables = pathVariablesMap.values.iterator()

    var bucketName = "endpoint.$id.${annotation.bucketName.ifEmpty { "${request.method} $matchedPath" }}"

    // Include route parameters to discriminate major routes, but not minor routes.
    // Example: These are different
    //  - /major/1/some-path -> "/major/{id}/some-path 1"
    //  - /major/2/some-path -> "/major/{id}/some-path 2"
    // However, for minor routes: These are the same
    //  - /major/1/minor/1/some-path -> "/major/{id}/minor/{subId}/some-path 1"
    //  - /major/1/minor/2/some-path -> "/major/{id}/minor/{subId}/some-path 1"

    var i = 0
    while (i < annotation.majorParametersToDiscriminate && pathVariables.hasNext()) {
      bucketName += pathVariables.next()
      i++
    }

    return RateLimitPolicy(
      bucketName,
      annotation.limit,
      annotation.windowSize
    )
  }
}
