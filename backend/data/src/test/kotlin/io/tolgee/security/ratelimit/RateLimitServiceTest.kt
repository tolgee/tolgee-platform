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
import io.tolgee.model.UserAccount
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.servlet.HandlerMapping
import java.time.Duration
import java.util.Date
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class RateLimitServiceTest {
  companion object {
    // Make these oddly specific numbers to make sure they're not coming from a hard-coded source
    const val TEST_IP_LIMIT = 1258773851
    const val TEST_IP_WINDOW = 8271325887L

    const val TEST_USER_LIMIT = 971251278
    const val TEST_USER_WINDOW = 4981230464L

    const val TEST_USER_ID = 1337L
  }

  private val cacheManager = ConcurrentMapCacheManager()

  private val lockingProvider = TestLockingProvider()

  private val currentDateProvider = Mockito.mock(CurrentDateProvider::class.java)

  private val rateLimitProperties = Mockito.spy(RateLimitProperties::class.java)

  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val rateLimitService =
    Mockito.spy(
      RateLimitService(
        cacheManager,
        lockingProvider,
        currentDateProvider,
        rateLimitProperties,
        authenticationFacade,
      ),
    )

  private val userAccount = Mockito.mock(UserAccount::class.java)

  @BeforeEach
  fun setupMocks() {
    val now = Date()
    Mockito.`when`(currentDateProvider.date).thenReturn(now)

    Mockito.`when`(userAccount.id).thenReturn(TEST_USER_ID)

    Mockito.`when`(rateLimitProperties.ipRequestLimit).thenReturn(TEST_IP_LIMIT)
    Mockito.`when`(rateLimitProperties.ipRequestWindow).thenReturn(TEST_IP_WINDOW)
    Mockito.`when`(rateLimitProperties.userRequestLimit).thenReturn(TEST_USER_LIMIT)
    Mockito.`when`(rateLimitProperties.userRequestWindow).thenReturn(TEST_USER_WINDOW)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(currentDateProvider, rateLimitProperties, userAccount)
  }

  @Test
  fun `it applies rate limit policies correctly`() {
    val testPolicy = RateLimitPolicy("test_policy", 2, Duration.ofSeconds(1), false)
    val baseTime = currentDateProvider.date.time

    rateLimitService.consumeBucket(testPolicy)
    rateLimitService.consumeBucket(testPolicy)
    val ex1 = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThat(ex1.retryAfter).isEqualTo(1000)

    Mockito.`when`(currentDateProvider.date).thenReturn(Date(baseTime + 500))
    val ex2 = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThat(ex2.retryAfter).isEqualTo(500)

    Mockito.`when`(currentDateProvider.date).thenReturn(Date(baseTime + 2_000))
    rateLimitService.consumeBucket(testPolicy)
  }

  @Test
  fun `it conditionally applies rate limit policies correctly`() {
    val testPolicy = RateLimitPolicy("test_policy", 2, Duration.ofSeconds(1), false)

    rateLimitService.consumeBucketUnless(testPolicy) { false }
    rateLimitService.consumeBucketUnless(testPolicy) { true }
    rateLimitService.consumeBucketUnless(testPolicy) { false }
    val ex1 = assertThrows<RateLimitedException> { rateLimitService.consumeBucketUnless(testPolicy) { false } }
    val ex2 = assertThrows<RateLimitedException> { rateLimitService.consumeBucketUnless(testPolicy) { true } }

    assertThat(ex1.retryAfter).isEqualTo(1000)
    assertThat(ex2.retryAfter).isEqualTo(1000)
  }

  @Test
  fun `it conditionally applies rate limit policies correctly when throwing exceptions`() {
    val testPolicy = RateLimitPolicy("test_policy", 2, Duration.ofSeconds(1), false)

    assertThrows<TestException> { rateLimitService.consumeBucketUnless(testPolicy) { throw TestException() } }
    rateLimitService.consumeBucketUnless(testPolicy) { true }
    assertThrows<TestException> { rateLimitService.consumeBucketUnless(testPolicy) { throw TestException() } }
    val ex1 =
      assertThrows<RateLimitedException> {
        rateLimitService.consumeBucketUnless(testPolicy) { throw TestException() }
      }
    val ex2 = assertThrows<RateLimitedException> { rateLimitService.consumeBucketUnless(testPolicy) { true } }

    assertThat(ex1.retryAfter).isEqualTo(1000)
    assertThat(ex2.retryAfter).isEqualTo(1000)
  }

  @Test
  fun `rate limit bucket is correctly defined for global public rate limit`() {
    val fakeRequest = makeFakeGenericRequest()
    val captor = argumentCaptor<RateLimitPolicy>()
    Mockito.`when`(rateLimitService.consumeBucket(captor.capture())).thenCallRealMethod()

    rateLimitService.consumeGlobalIpRateLimitPolicy(fakeRequest)

    val policy = captor.firstValue
    assertThat(policy).isNotNull
    assertThat(policy.bucketName).isEqualTo("global.ip.127.0.0.1")
    assertThat(policy.limit).isEqualTo(TEST_IP_LIMIT)
    assertThat(policy.refillDuration).isEqualTo(Duration.ofMillis(TEST_IP_WINDOW))
  }

  @Test
  fun `rate limit bucket is correctly defined for global user rate limit`() {
    val fakeRequest = makeFakeGenericRequest()
    val captor = argumentCaptor<RateLimitPolicy>()
    Mockito.`when`(rateLimitService.consumeBucket(captor.capture())).thenCallRealMethod()

    rateLimitService.consumeGlobalUserRateLimitPolicy(fakeRequest, userAccount.id)

    val policy = captor.firstValue
    assertThat(policy).isNotNull
    assertThat(policy.bucketName).isEqualTo("global.user.$TEST_USER_ID")
    assertThat(policy.limit).isEqualTo(TEST_USER_LIMIT)
    assertThat(policy.refillDuration).isEqualTo(Duration.ofMillis(TEST_USER_WINDOW))
  }

  @Test
  fun `rate limit bucket is correctly defined for global auth rate limit`() {
    val fakeRequest = makeFakeGenericRequest()
    val policy = rateLimitService.getIpAuthRateLimitPolicy(fakeRequest)

    assertThat(policy).isNotNull
    assertThat(policy?.bucketName).isEqualTo("global.ip.127.0.0.1::auth")
  }

  @Test
  fun `global-limits config only affects global limits`() {
    Mockito.`when`(rateLimitProperties.globalLimits).thenReturn(false)

    val fakeRequest = makeFakeGenericRequest()

    rateLimitService.consumeGlobalIpRateLimitPolicy(fakeRequest)
    rateLimitService.consumeGlobalUserRateLimitPolicy(fakeRequest, userAccount.id)
    val authPolicy = rateLimitService.getIpAuthRateLimitPolicy(fakeRequest)

    Mockito.verify(rateLimitService, times(0)).consumeBucket(any())
    assertThat(authPolicy).isNotNull
  }

  @Test
  fun `endpoint-limits config only affects non-auth endpoint limits`() {
    Mockito.`when`(rateLimitProperties.endpointLimits).thenReturn(false)

    val fakeRequest = makeFakeGenericRequest()

    rateLimitService.consumeGlobalIpRateLimitPolicy(fakeRequest)
    rateLimitService.consumeGlobalUserRateLimitPolicy(fakeRequest, userAccount.id)
    val authPolicy = rateLimitService.getIpAuthRateLimitPolicy(fakeRequest)

    Mockito.verify(rateLimitService, times(2)).consumeBucket(any())
    assertThat(authPolicy).isNotNull
  }

  @Test
  fun `authentication-limits config only affects authentication endpoint limits`() {
    Mockito.`when`(rateLimitProperties.authenticationLimits).thenReturn(false)

    val fakeRequest = makeFakeGenericRequest()

    rateLimitService.consumeGlobalIpRateLimitPolicy(fakeRequest)
    rateLimitService.consumeGlobalUserRateLimitPolicy(fakeRequest, userAccount.id)
    val authPolicy = rateLimitService.getIpAuthRateLimitPolicy(fakeRequest)

    Mockito.verify(rateLimitService, times(2)).consumeBucket(any())
    assertThat(authPolicy).isNull()
  }

  @Test
  fun `strike count increments on rate limit violations`() {
    Mockito.`when`(rateLimitProperties.maxStrikesBeforeBlock).thenReturn(5)
    Mockito.`when`(rateLimitProperties.strikeResetWindowMs).thenReturn(60_000L)

    val testPolicy = RateLimitPolicy("strike_test", 2, Duration.ofSeconds(10), false)

    // Use up tokens
    rateLimitService.consumeBucket(testPolicy)
    rateLimitService.consumeBucket(testPolicy)

    // First violation - strike 1
    val ex1 = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThat(ex1.strikeCount).isEqualTo(1)

    // Second violation - strike 2
    val ex2 = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThat(ex2.strikeCount).isEqualTo(2)

    // Third violation - strike 3
    val ex3 = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThat(ex3.strikeCount).isEqualTo(3)
  }

  @Test
  fun `strikes reset after strike window passes`() {
    val baseTime = currentDateProvider.date.time
    Mockito.`when`(rateLimitProperties.maxStrikesBeforeBlock).thenReturn(5)
    Mockito.`when`(rateLimitProperties.strikeResetWindowMs).thenReturn(1000L) // 1 second window

    val testPolicy = RateLimitPolicy("strike_reset_test", 1, Duration.ofSeconds(10), false)

    // Consume the only token
    rateLimitService.consumeBucket(testPolicy)

    // Get a strike
    val ex1 = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThat(ex1.strikeCount).isEqualTo(1)

    // Get another strike
    val ex2 = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThat(ex2.strikeCount).isEqualTo(2)

    // Advance time past the strike window
    Mockito.`when`(currentDateProvider.date).thenReturn(Date(baseTime + 1500))

    // Strike count should reset
    val ex3 = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThat(ex3.strikeCount).isEqualTo(1)
  }

  @Test
  fun `connection dropped after max strikes exceeded`() {
    Mockito.`when`(rateLimitProperties.maxStrikesBeforeBlock).thenReturn(3)
    Mockito.`when`(rateLimitProperties.strikeResetWindowMs).thenReturn(60_000L)

    val testPolicy = RateLimitPolicy("block_test", 1, Duration.ofSeconds(10), false)

    // Use up the token
    rateLimitService.consumeBucket(testPolicy)

    // Get 3 strikes (max allowed)
    assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }

    // 4th violation should trigger block
    assertThrows<RateLimitBlockedException> { rateLimitService.consumeBucket(testPolicy) }
  }

  @Test
  fun `connection dropping disabled when maxStrikesBeforeBlock is 0`() {
    Mockito.`when`(rateLimitProperties.maxStrikesBeforeBlock).thenReturn(0)
    Mockito.`when`(rateLimitProperties.strikeResetWindowMs).thenReturn(60_000L)

    val testPolicy = RateLimitPolicy("no_block_test", 1, Duration.ofSeconds(10), false)

    // Use up the token
    rateLimitService.consumeBucket(testPolicy)

    // Should always throw RateLimitedException, never RateLimitBlockedException
    repeat(10) {
      val ex = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
      assertThat(ex.strikeCount).isEqualTo(it + 1)
    }
  }

  @Test
  fun `strikes persist across bucket refills`() {
    val baseTime = currentDateProvider.date.time
    Mockito.`when`(rateLimitProperties.maxStrikesBeforeBlock).thenReturn(5)
    Mockito.`when`(rateLimitProperties.strikeResetWindowMs).thenReturn(60_000L)

    val testPolicy = RateLimitPolicy("refill_strike_test", 1, Duration.ofSeconds(1), false)

    // Use up token and get a strike
    rateLimitService.consumeBucket(testPolicy)
    val ex1 = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThat(ex1.strikeCount).isEqualTo(1)

    // Wait for bucket refill but not strike window
    Mockito.`when`(currentDateProvider.date).thenReturn(Date(baseTime + 2000))

    // Use up token and get another strike - count should continue from 1
    rateLimitService.consumeBucket(testPolicy)
    val ex2 = assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }
    assertThat(ex2.strikeCount).isEqualTo(2)
  }

  // --- HELPERS
  private fun makeFakeGenericRequest(): MockHttpServletRequest {
    val fakeRequest = MockHttpServletRequest()
    fakeRequest.remoteAddr = "127.0.0.1"
    fakeRequest.method = "GET"
    fakeRequest.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, emptyMap<String, String>())
    fakeRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/fake/route")
    return fakeRequest
  }

  // Accessing the one from API package is a pain here.
  class TestLockingProvider : LockingProvider {
    private val lock = ReentrantLock()

    override fun getLock(name: String): Lock = lock

    override fun <T> withLocking(
      name: String,
      fn: () -> T,
    ): T {
      lock.lock()
      try {
        return fn()
      } finally {
        lock.unlock()
      }
    }

    override fun <T> withLockingIfFree(
      name: String,
      leaseTime: java.time.Duration,
      fn: () -> T,
    ): T? {
      TODO("Not yet implemented")
    }
  }

  class TestException : Exception()
}
