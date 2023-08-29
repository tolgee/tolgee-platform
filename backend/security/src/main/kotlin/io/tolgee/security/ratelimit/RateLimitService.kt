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
  private val rateLimitProperties: RateLimitProperties,
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
      val bucket = cache.get(policy.bucketName, Bucket::class.java)
      cache.put(policy.bucketName, doConsumeBucket(policy, bucket))
    }
  }

  /**
   * Consumes a token from the provided rate limit policy unless a condition is met.
   * The bucket must contain at least 1 token even if the condition would evaluate to true.
   * The condition can throw, and the result will be considered "false".
   *
   * @param policy The rate limit policy.
   * @throws RateLimitedException There are no longer any tokens in the bucket.
   */
  fun consumeBucketUnless(policy: RateLimitPolicy, cond: () -> Boolean) {
    lockingProvider.withLocking("tolgee.ratelimit.${policy.bucketName}") {
      val bucket = cache.get(policy.bucketName, Bucket::class.java)
      val consumed = doConsumeBucket(policy, bucket)
      try {
        if (!cond()) {
          cache.put(policy.bucketName, consumed)
        }
      } catch (e: Exception) {
        cache.put(policy.bucketName, consumed)
        throw e
      }
    }
  }

  /**
   * Returns the global per-ip rate limit policy applicable to the request.
   *
   * @param request The HTTP request.
   * @return The applicable rate limit policy, if any.
   */
  fun getGlobalIpRateLimitPolicy(request: HttpServletRequest): RateLimitPolicy? {
    @Suppress("DEPRECATION") // TODO: remove for Tolgee 4 release
    if (!rateLimitProperties.enabled) return null
    if (!rateLimitProperties.globalLimits) return null

    return RateLimitPolicy(
      "global.ip.${request.remoteAddr}",
      rateLimitProperties.ipRequestLimit,
      rateLimitProperties.ipRequestWindow,
      true,
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
    @Suppress("DEPRECATION") // TODO: remove for Tolgee 4 release
    if (!rateLimitProperties.enabled) return null
    if (!rateLimitProperties.globalLimits) return null

    return RateLimitPolicy(
      "global.user.${account.id}",
      rateLimitProperties.userRequestLimit,
      rateLimitProperties.userRequestWindow,
      true,
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
  fun getEndpointRateLimit(
    request: HttpServletRequest,
    account: UserAccount?,
    handler: HandlerMethod
  ): RateLimitPolicy? {
    @Suppress("DEPRECATION") // TODO: remove for Tolgee 4 release
    if (!rateLimitProperties.enabled) return null

    val annotation = AnnotationUtils.getAnnotation(handler.method, RateLimited::class.java)
      ?: return null

    if (
      (!annotation.isAuthentication && !rateLimitProperties.endpointLimits) ||
      (annotation.isAuthentication && !rateLimitProperties.authenticationLimits)
    ) {
      return null
    }

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
      annotation.windowSize,
      false,
    )
  }

  /**
   * Returns the per-ip rate limit policy applicable to the authentication phase.
   *
   * @param request The HTTP request.
   * @return The applicable rate limit policy, if any.
   */
  fun getIpAuthRateLimitPolicy(request: HttpServletRequest): RateLimitPolicy? {
    @Suppress("DEPRECATION") // TODO: remove for Tolgee 4 release
    if (!rateLimitProperties.enabled) return null
    if (!rateLimitProperties.authenticationLimits) return null

    return RateLimitPolicy(
      "global.ip.${request.remoteAddr}::auth",
      5,
      1000,
      true,
    )
  }

  /**
   * Consumes a token from a bucket according to the rate limit policy.
   *
   * @param policy The rate limit policy.
   * @param bucket The bucket to consume. Can be null or expired.
   * @return The updated rate limit bucket.
   * @throws RateLimitedException There are no longer any tokens in the bucket.
   */
  private fun doConsumeBucket(policy: RateLimitPolicy, bucket: Bucket?): Bucket {
    val time = currentDateProvider.date.time
    if (bucket == null || bucket.resetAt < time) {
      val tokensRemaining = policy.limit - 1
      val tokensResetAt = time + policy.windowSize

      return Bucket(tokensRemaining, tokensResetAt)
    }

    if (bucket.tokens == 0) {
      throw RateLimitedException(bucket.resetAt - time, policy.global)
    }

    return Bucket(bucket.tokens - 1, bucket.resetAt)
  }
}
