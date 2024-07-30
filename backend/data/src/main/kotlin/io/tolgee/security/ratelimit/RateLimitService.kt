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
import io.tolgee.configuration.tolgee.RateLimitProperties
import io.tolgee.constants.Caches
import jakarta.servlet.http.HttpServletRequest
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.time.Duration

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
  fun consumeBucket(policy: RateLimitPolicy?) {
    consumeBucketUnless(policy) { false }
  }

  /**
   * Consumes a token from the provided rate limit policy unless a condition is met.
   * The bucket must contain at least 1 token even if the condition would evaluate to true.
   * The condition can throw, and the result will be considered "false".
   *
   * @param policy The rate limit policy.
   * @throws RateLimitedException There are no longer any tokens in the bucket.
   */
  fun consumeBucketUnless(
    policy: RateLimitPolicy?,
    cond: () -> Boolean,
  ) {
    if (policy == null) return

    val lockName = getLockName(policy)
    lockingProvider.withLocking(lockName) {
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
  fun consumeGlobalIpRateLimitPolicy(request: HttpServletRequest) {
    @Suppress("DEPRECATION") // TODO: remove for Tolgee 4 release
    if (!rateLimitProperties.enabled) return
    if (!rateLimitProperties.globalLimits) return

    consumeBucket(
      RateLimitPolicy(
        "global.ip.${request.remoteAddr}",
        rateLimitProperties.ipRequestLimit,
        Duration.ofMillis(rateLimitProperties.ipRequestWindow),
        true,
      ),
    )
  }

  /**
   * Consumes a bucket according to the global per-user rate limit policy applicable to the request.
   *
   * @param request The HTTP request.
   * @param userId The authenticated user ID.
   * @return The applicable rate limit policy, if any.
   */
  fun consumeGlobalUserRateLimitPolicy(
    request: HttpServletRequest,
    userId: Long,
  ) {
    @Suppress("DEPRECATION") // TODO: remove for Tolgee 4 release
    if (!rateLimitProperties.enabled) return
    if (!rateLimitProperties.globalLimits) return

    consumeBucket(
      RateLimitPolicy(
        "global.user.$userId",
        rateLimitProperties.userRequestLimit,
        Duration.ofMillis(rateLimitProperties.userRequestWindow),
        true,
      ),
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
      Duration.ofSeconds(1),
      true,
    )
  }

  fun getEmailVerificationIpRateLimitPolicy(
    request: HttpServletRequest,
    email: String?,
  ): RateLimitPolicy? {
    if (!rateLimitProperties.emailVerificationRequestLimitEnabled || email.isNullOrEmpty()) return null

    val ip = request.remoteAddr
    val key = "global.ip.$ip::email_verification"

    return RateLimitPolicy(
      key,
      rateLimitProperties.emailVerificationRequestLimit,
      Duration.ofMillis(rateLimitProperties.emailVerificationRequestWindow),
      false,
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
  private fun doConsumeBucket(
    policy: RateLimitPolicy,
    bucket: Bucket?,
  ): Bucket {
    val time = currentDateProvider.date.time
    if (bucket == null || bucket.refillAt < time) {
      val tokensRemaining = policy.limit - 1
      val tokensResetAt = policy.refillDuration.toMillis() + time

      return Bucket(tokensRemaining, tokensResetAt)
    }

    if (bucket.tokens == 0) {
      throw RateLimitedException(bucket.refillAt - time, policy.global)
    }

    return Bucket(bucket.tokens - 1, bucket.refillAt)
  }

  private fun getLockName(policy: RateLimitPolicy): String {
    return "tolgee.ratelimit.${policy.bucketName}"
  }
}
