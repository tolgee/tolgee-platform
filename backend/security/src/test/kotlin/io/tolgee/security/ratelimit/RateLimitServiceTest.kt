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
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

  private val rateLimitService = RateLimitService(
    cacheManager,
    lockingProvider,
    currentDateProvider,
    rateLimitProperties,
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
    val testPolicy = RateLimitPolicy("test_policy", 2, 1_000, false)
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
    val testPolicy = RateLimitPolicy("test_policy", 2, 1_000, false)

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
    val testPolicy = RateLimitPolicy("test_policy", 2, 1_000, false)

    assertThrows<TestException> { rateLimitService.consumeBucketUnless(testPolicy) { throw TestException() } }
    rateLimitService.consumeBucketUnless(testPolicy) { true }
    assertThrows<TestException> { rateLimitService.consumeBucketUnless(testPolicy) { throw TestException() } }
    val ex1 = assertThrows<RateLimitedException> {
      rateLimitService.consumeBucketUnless(testPolicy) { throw TestException() }
    }
    val ex2 = assertThrows<RateLimitedException> { rateLimitService.consumeBucketUnless(testPolicy) { true } }

    assertThat(ex1.retryAfter).isEqualTo(1000)
    assertThat(ex2.retryAfter).isEqualTo(1000)
  }

  @Test
  fun `rate limit bucket is correctly defined for global public rate limit`() {
    val fakeRequest = makeFakeGenericRequest()
    val policy = rateLimitService.getGlobalIpRateLimitPolicy(fakeRequest)

    assertThat(policy).isNotNull
    assertThat(policy?.bucketName).isEqualTo("global.ip.127.0.0.1")
    assertThat(policy?.limit).isEqualTo(TEST_IP_LIMIT)
    assertThat(policy?.windowSize).isEqualTo(TEST_IP_WINDOW)
  }

  @Test
  fun `rate limit bucket is correctly defined for global user rate limit`() {
    val fakeRequest = makeFakeGenericRequest()
    val policy = rateLimitService.getGlobalUserRateLimitPolicy(fakeRequest, userAccount)

    assertThat(policy).isNotNull
    assertThat(policy?.bucketName).isEqualTo("global.user.$TEST_USER_ID")
    assertThat(policy?.limit).isEqualTo(TEST_USER_LIMIT)
    assertThat(policy?.windowSize).isEqualTo(TEST_USER_WINDOW)
  }

  @Test
  fun `rate limit bucket is correctly defined for global auth rate limit`() {
    val fakeRequest = makeFakeGenericRequest()
    val policy = rateLimitService.getIpAuthRateLimitPolicy(fakeRequest)

    assertThat(policy).isNotNull
    assertThat(policy?.bucketName).isEqualTo("global.ip.127.0.0.1::auth")
  }

  @Test
  fun `endpoint rate limit policy is correctly extracted from annotations`() {
    val fakeRequest = makeFakeGenericRequest()

    val handlerWithoutLimit = HandlerMethod("", FakeController::fakeHandlerWithoutRateLimit.javaMethod!!)
    val handlerWithoutBucket = HandlerMethod("", FakeController::fakeHandler.javaMethod!!)
    val handlerWithBucket = HandlerMethod("", FakeController::fakeHandlerWithExplicitBucket.javaMethod!!)
    val handlerInherit = HandlerMethod("", FakeController::fakeHandlerInherit.javaMethod!!)

    val noPolicy = rateLimitService.getEndpointRateLimit(fakeRequest, null, handlerWithoutLimit)
    val policy1 = rateLimitService.getEndpointRateLimit(fakeRequest, null, handlerWithoutBucket)
    val policy2 = rateLimitService.getEndpointRateLimit(fakeRequest, null, handlerWithBucket)
    val policy3 = rateLimitService.getEndpointRateLimit(fakeRequest, null, handlerInherit)

    assertThat(noPolicy).isNull()
    assertThat(policy1).isNotNull
    assertThat(policy2).isNotNull
    assertThat(policy3).isNotNull

    assertThat(policy1?.limit).isEqualTo(2)
    assertThat(policy2?.limit).isEqualTo(2)
    assertThat(policy3?.limit).isEqualTo(2)

    assertThat(policy1?.bucketName).isEqualTo("endpoint.ip.127.0.0.1.GET /fake/route")
    assertThat(policy2?.bucketName).isEqualTo("endpoint.ip.127.0.0.1.uwu")
    assertThat(policy3?.bucketName).isEqualTo("endpoint.ip.127.0.0.1.GET /fake/route")
  }

  @Test
  fun `endpoint rate limit bucket correctly discriminates against major route parameters`() {
    val fakeRequest1 = makeFakeGenericRequest()
    fakeRequest1.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mapOf("major" to "1", "minor" to "2"))
    fakeRequest1.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/fake/route/{major}/{minor}")

    val fakeRequest2 = makeFakeGenericRequest()
    fakeRequest2.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mapOf("major" to "2", "minor" to "2"))
    fakeRequest2.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/fake/route/{major}/{minor}")

    val fakeRequest3 = makeFakeGenericRequest()
    fakeRequest3.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mapOf("major" to "2", "minor" to "3"))
    fakeRequest3.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/fake/route/{major}/{minor}")

    val handlerWithMajor = HandlerMethod("", FakeController::fakeHandler.javaMethod!!)
    val handlerWithoutMajor = HandlerMethod("", FakeController::fakeHandlerWithoutMajorDiscriminator.javaMethod!!)

    val policyWithMajor1 = rateLimitService.getEndpointRateLimit(fakeRequest1, null, handlerWithMajor)
    val policyWithMajor2 = rateLimitService.getEndpointRateLimit(fakeRequest2, null, handlerWithMajor)
    val policyWithMajor3 = rateLimitService.getEndpointRateLimit(fakeRequest3, null, handlerWithMajor)

    val policyWithoutMajor1 = rateLimitService.getEndpointRateLimit(fakeRequest1, null, handlerWithoutMajor)
    val policyWithoutMajor2 = rateLimitService.getEndpointRateLimit(fakeRequest2, null, handlerWithoutMajor)

    assertThat(policyWithMajor1).isNotNull
    assertThat(policyWithMajor2).isNotNull
    assertThat(policyWithMajor3).isNotNull
    assertThat(policyWithoutMajor1).isNotNull
    assertThat(policyWithoutMajor2).isNotNull

    assertThat(policyWithMajor1?.bucketName).isNotEqualTo(policyWithMajor2?.bucketName)
    assertThat(policyWithoutMajor1?.bucketName).isEqualTo(policyWithoutMajor2?.bucketName)
    assertThat(policyWithMajor2?.bucketName).isEqualTo(policyWithMajor3?.bucketName)
  }

  @Test
  fun `endpoint rate limit uses the correct user or ip discrimination method`() {
    val fakeRequest = makeFakeGenericRequest()
    val handler = HandlerMethod("", FakeController::fakeHandler.javaMethod!!)

    val policy1 = rateLimitService.getEndpointRateLimit(fakeRequest, null, handler)
    val policy2 = rateLimitService.getEndpointRateLimit(fakeRequest, userAccount, handler)

    assertThat(policy1).isNotNull
    assertThat(policy2).isNotNull

    assertThat(policy1?.bucketName).isEqualTo("endpoint.ip.127.0.0.1.GET /fake/route")
    assertThat(policy2?.bucketName).isEqualTo("endpoint.user.1337.GET /fake/route")
  }

  @Test
  fun `global-limits config only affects global limits`() {
    Mockito.`when`(rateLimitProperties.globalLimits).thenReturn(false)

    val fakeRequest = makeFakeGenericRequest()

    val handlerGeneric = HandlerMethod("", FakeController::fakeHandler.javaMethod!!)
    val handlerAuth = HandlerMethod("", FakeController::fakeAuthHandler.javaMethod!!)

    val globalIpPolicy = rateLimitService.getGlobalIpRateLimitPolicy(fakeRequest)
    val globalUserPolicy = rateLimitService.getGlobalUserRateLimitPolicy(fakeRequest, userAccount)
    val genericEndpointPolicy = rateLimitService.getEndpointRateLimit(fakeRequest, userAccount, handlerGeneric)
    val authEndpointPolicy = rateLimitService.getEndpointRateLimit(fakeRequest, userAccount, handlerAuth)
    val authPolicy = rateLimitService.getIpAuthRateLimitPolicy(fakeRequest)

    assertThat(globalIpPolicy).isNull()
    assertThat(globalUserPolicy).isNull()
    assertThat(genericEndpointPolicy).isNotNull
    assertThat(authEndpointPolicy).isNotNull
    assertThat(authPolicy).isNotNull
  }

  @Test
  fun `endpoint-limits config only affects non-auth endpoint limits`() {
    Mockito.`when`(rateLimitProperties.endpointLimits).thenReturn(false)

    val fakeRequest = makeFakeGenericRequest()

    val handlerGeneric = HandlerMethod("", FakeController::fakeHandler.javaMethod!!)
    val handlerAuth = HandlerMethod("", FakeController::fakeAuthHandler.javaMethod!!)

    val globalIpPolicy = rateLimitService.getGlobalIpRateLimitPolicy(fakeRequest)
    val globalUserPolicy = rateLimitService.getGlobalUserRateLimitPolicy(fakeRequest, userAccount)
    val genericEndpointPolicy = rateLimitService.getEndpointRateLimit(fakeRequest, userAccount, handlerGeneric)
    val authEndpointPolicy = rateLimitService.getEndpointRateLimit(fakeRequest, userAccount, handlerAuth)
    val authPolicy = rateLimitService.getIpAuthRateLimitPolicy(fakeRequest)

    assertThat(globalIpPolicy).isNotNull
    assertThat(globalUserPolicy).isNotNull
    assertThat(genericEndpointPolicy).isNull()
    assertThat(authEndpointPolicy).isNotNull
    assertThat(authPolicy).isNotNull
  }

  @Test
  fun `authentication-limits config only affects authentication endpoint limits`() {
    Mockito.`when`(rateLimitProperties.authenticationLimits).thenReturn(false)

    val fakeRequest = makeFakeGenericRequest()

    val handlerGeneric = HandlerMethod("", FakeController::fakeHandler.javaMethod!!)
    val handlerAuth = HandlerMethod("", FakeController::fakeAuthHandler.javaMethod!!)

    val globalIpPolicy = rateLimitService.getGlobalIpRateLimitPolicy(fakeRequest)
    val globalUserPolicy = rateLimitService.getGlobalUserRateLimitPolicy(fakeRequest, userAccount)
    val genericEndpointPolicy = rateLimitService.getEndpointRateLimit(fakeRequest, userAccount, handlerGeneric)
    val authEndpointPolicy = rateLimitService.getEndpointRateLimit(fakeRequest, userAccount, handlerAuth)
    val authPolicy = rateLimitService.getIpAuthRateLimitPolicy(fakeRequest)

    assertThat(globalIpPolicy).isNotNull
    assertThat(globalUserPolicy).isNotNull
    assertThat(genericEndpointPolicy).isNotNull
    assertThat(authEndpointPolicy).isNull()
    assertThat(authPolicy).isNull()
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

  class TestLockingProvider : LockingProvider {
    private val lock = ReentrantLock()
    override fun getLock(name: String): Lock = lock
  }

  @RateLimited(2)
  annotation class InheritedRateLimit

  class TestException: Exception()

  class FakeController {
    @RateLimited(2)
    fun fakeHandler() {}

    @RateLimited(2, bucketName = "uwu")
    fun fakeHandlerWithExplicitBucket() {}

    @RateLimited(2, majorParametersToDiscriminate = 0)
    fun fakeHandlerWithoutMajorDiscriminator() {}

    fun fakeHandlerWithoutRateLimit() {}

    @RateLimited(2, isAuthentication = true)
    fun fakeAuthHandler() {}

    @InheritedRateLimit
    fun fakeHandlerInherit() {}
  }
}
