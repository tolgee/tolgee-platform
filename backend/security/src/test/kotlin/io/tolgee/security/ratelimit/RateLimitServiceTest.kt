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
import io.tolgee.model.UserAccount
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.reflect.jvm.javaMethod

class RateLimitServiceTest {
  // TODO: use an internal fake cache manager that uses thread-local caches - this would allow these tests to be run in parallel
  private val cacheManager = ConcurrentMapCacheManager()
  // TODO: use an internal fake locking provider that uses thread-local locks - same reason as above
  private val lockingProvider = TestLockingProvider()

  @Mock
  private val currentDateProvider: CurrentDateProvider = Mockito.mock(CurrentDateProvider::class.java)

  private val rateLimitService = RateLimitService(cacheManager, lockingProvider, currentDateProvider)

  @BeforeEach
  fun setupMocks() {
    val now = Date()
    Mockito.`when`(currentDateProvider.date).thenReturn(now)
  }

  @Test
  fun `it applies rate limit policies correctly`() {
    val testPolicy = RateLimitPolicy("test_policy", 2, 1_000)

    rateLimitService.consumeBucket(testPolicy)
    rateLimitService.consumeBucket(testPolicy)
    assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }

    Mockito.`when`(currentDateProvider.date).thenReturn(Date(System.currentTimeMillis() + 500))
    assertThrows<RateLimitedException> { rateLimitService.consumeBucket(testPolicy) }

    Mockito.`when`(currentDateProvider.date).thenReturn(Date(System.currentTimeMillis() + 2_000))
    rateLimitService.consumeBucket(testPolicy)
  }

  @Test
  fun `rate limit bucket is correctly defined for global public rate limit`() {
    val fakeRequest = MockHttpServletRequest()
    fakeRequest.remoteAddr = "127.0.0.1"

    val policy = rateLimitService.getGlobalIpRateLimitPolicy(fakeRequest)

    Assertions.assertThat(policy).isNotNull
    Assertions.assertThat(policy?.bucketName).isEqualTo("global.ip.127.0.0.1")
  }

  @Test
  fun `rate limit bucket is correctly defined for global user rate limit`() {
    val fakeRequest = MockHttpServletRequest()
    val userAccount = Mockito.mock(UserAccount::class.java)
    Mockito.`when`(userAccount.id).thenReturn(1337L)
    fakeRequest.remoteAddr = "127.0.0.1"

    val policy = rateLimitService.getGlobalUserRateLimitPolicy(fakeRequest, userAccount)

    Assertions.assertThat(policy).isNotNull
    Assertions.assertThat(policy?.bucketName).isEqualTo("global.user.1337")
  }

  @Test
  fun `endpoint rate limit policy is correctly extracted from annotations`() {
    val fakeRequest = MockHttpServletRequest()
    fakeRequest.remoteAddr = "127.0.0.1"
    fakeRequest.method = "GET"
    fakeRequest.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, emptyMap<String, String>())
    fakeRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/fake/route")

    val handlerWithoutLimit = HandlerMethod("", FakeController::fakeHandlerWithoutRateLimit.javaMethod!!)
    val handlerWithoutBucket = HandlerMethod("", FakeController::fakeHandler.javaMethod!!)
    val handlerWithBucket = HandlerMethod("", FakeController::fakeHandlerWithExplicitBucket.javaMethod!!)

    val noPolicy = rateLimitService.getEndpointRateLimit(fakeRequest, null, handlerWithoutLimit)
    val policy1 = rateLimitService.getEndpointRateLimit(fakeRequest, null, handlerWithoutBucket)
    val policy2 = rateLimitService.getEndpointRateLimit(fakeRequest, null, handlerWithBucket)

    Assertions.assertThat(noPolicy).isNull()
    Assertions.assertThat(policy1).isNotNull
    Assertions.assertThat(policy2).isNotNull

    Assertions.assertThat(policy1?.limit).isEqualTo(2)
    Assertions.assertThat(policy2?.limit).isEqualTo(2)

    Assertions.assertThat(policy1?.bucketName).isEqualTo("endpoint.ip.127.0.0.1.GET /fake/route")
    Assertions.assertThat(policy2?.bucketName).isEqualTo("endpoint.ip.127.0.0.1.uwu")
  }

  @Test
  fun `endpoint rate limit bucket correctly discriminates against major route parameters`() {
    val fakeRequest1 = MockHttpServletRequest()
    fakeRequest1.remoteAddr = "127.0.0.1"
    fakeRequest1.method = "GET"
    fakeRequest1.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mapOf("major" to "1", "minor" to "2"))
    fakeRequest1.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/fake/route/{major}/{minor}")

    val fakeRequest2 = MockHttpServletRequest()
    fakeRequest2.remoteAddr = "127.0.0.1"
    fakeRequest2.method = "GET"
    fakeRequest2.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mapOf("major" to "2", "minor" to "2"))
    fakeRequest2.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/fake/route/{major}/{minor}")

    val fakeRequest3 = MockHttpServletRequest()
    fakeRequest3.remoteAddr = "127.0.0.1"
    fakeRequest3.method = "GET"
    fakeRequest3.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mapOf("major" to "2", "minor" to "3"))
    fakeRequest3.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/fake/route/{major}/{minor}")

    val handlerWithMajor = HandlerMethod("", FakeController::fakeHandler.javaMethod!!)
    val handlerWithoutMajor = HandlerMethod("", FakeController::fakeHandlerWithoutMajorDiscriminator.javaMethod!!)

    val policyWithMajor1 = rateLimitService.getEndpointRateLimit(fakeRequest1, null, handlerWithMajor)
    val policyWithMajor2 = rateLimitService.getEndpointRateLimit(fakeRequest2, null, handlerWithMajor)
    val policyWithMajor3 = rateLimitService.getEndpointRateLimit(fakeRequest3, null, handlerWithMajor)

    val policyWithoutMajor1 = rateLimitService.getEndpointRateLimit(fakeRequest1, null, handlerWithoutMajor)
    val policyWithoutMajor2 = rateLimitService.getEndpointRateLimit(fakeRequest2, null, handlerWithoutMajor)

    Assertions.assertThat(policyWithMajor1).isNotNull
    Assertions.assertThat(policyWithMajor2).isNotNull
    Assertions.assertThat(policyWithMajor3).isNotNull
    Assertions.assertThat(policyWithoutMajor1).isNotNull
    Assertions.assertThat(policyWithoutMajor2).isNotNull

    Assertions.assertThat(policyWithMajor1?.bucketName).isNotEqualTo(policyWithMajor2?.bucketName)
    Assertions.assertThat(policyWithoutMajor1?.bucketName).isEqualTo(policyWithoutMajor2?.bucketName)
    Assertions.assertThat(policyWithMajor2?.bucketName).isEqualTo(policyWithMajor3?.bucketName)
  }

  @Test
  fun `endpoint rate limit uses the correct user or ip discrimination method`() {
    val fakeRequest = MockHttpServletRequest()
    fakeRequest.remoteAddr = "127.0.0.1"
    fakeRequest.method = "GET"
    fakeRequest.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, emptyMap<String, String>())
    fakeRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/fake/route")

    val userAccount = Mockito.mock(UserAccount::class.java)
    Mockito.`when`(userAccount.id).thenReturn(1337L)

    val handler = HandlerMethod("", FakeController::fakeHandler.javaMethod!!)

    val policy1 = rateLimitService.getEndpointRateLimit(fakeRequest, null, handler)
    val policy2 = rateLimitService.getEndpointRateLimit(fakeRequest, userAccount, handler)

    Assertions.assertThat(policy1).isNotNull
    Assertions.assertThat(policy2).isNotNull

    Assertions.assertThat(policy1?.bucketName).isEqualTo("endpoint.ip.127.0.0.1.GET /fake/route")
    Assertions.assertThat(policy2?.bucketName).isEqualTo("endpoint.user.1337.GET /fake/route")
  }

  class TestLockingProvider: LockingProvider {
    private val lock = ReentrantLock()
    override fun getLock(name: String): Lock = lock
  }

  class FakeController {
    @RateLimited(2)
    fun fakeHandler() {}

    @RateLimited(2, bucketName = "uwu")
    fun fakeHandlerWithExplicitBucket() {}

    @RateLimited(2, majorParametersToDiscriminate = 0)
    fun fakeHandlerWithoutMajorDiscriminator() {}

    fun fakeHandlerWithoutRateLimit() {}
  }
}
