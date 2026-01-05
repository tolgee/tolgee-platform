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
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andIsRateLimited
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping
import java.util.Date
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.reflect.jvm.javaMethod

class RateLimitInterceptorTest {
  private val rateLimitProperties = Mockito.spy(RateLimitProperties::class.java)

  private val currentDateProvider = Mockito.mock(CurrentDateProvider::class.java)

  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val userAccount = Mockito.mock(UserAccountDto::class.java)

  private val rateLimitService =
    Mockito.spy(
      RateLimitService(
        ConcurrentMapCacheManager(),
        TestLockingProvider(),
        currentDateProvider,
        rateLimitProperties,
        authenticationFacade,
      ),
    )

  private val rateLimitInterceptor =
    RateLimitInterceptor(
      authenticationFacade,
      rateLimitService,
    )

  private val mockMvc =
    MockMvcBuilders
      .standaloneSetup(TestController::class.java)
      .addInterceptors(rateLimitInterceptor)
      .build()

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(currentDateProvider.date).thenReturn(Date())
    Mockito.`when`(userAccount.id).thenReturn(1337L)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(rateLimitProperties, currentDateProvider, authenticationFacade, userAccount)
  }

  @Test
  fun `it does not rate limit when there are no annotations`() {
    mockMvc.perform(get("/")).andIsOk
    mockMvc.perform(get("/")).andIsOk
    mockMvc.perform(get("/")).andIsOk

    Mockito.verify(rateLimitService, Mockito.never()).consumeBucket(any())
  }

  @Test
  fun `it rate limits requests according to the specified policy`() {
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsRateLimited

    Mockito.verify(rateLimitService, Mockito.times(3)).consumeBucket(any())
  }

  @Test
  fun `it uses different buckets for different paths`() {
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsRateLimited

    mockMvc.perform(get("/rate-limited-2")).andIsOk
    mockMvc.perform(get("/rate-limited-2")).andIsOk
    mockMvc.perform(get("/rate-limited-2")).andIsRateLimited
  }

  @Test
  fun `it uses the same buckets for paths with a shared bucket`() {
    mockMvc.perform(get("/rate-limited-shared")).andIsOk
    mockMvc.perform(get("/rate-limited-shared")).andIsOk
    mockMvc.perform(get("/rate-limited-shared")).andIsRateLimited
    mockMvc.perform(get("/rate-limited-shared-2")).andIsRateLimited

    val now = currentDateProvider.date.time
    Mockito.`when`(currentDateProvider.date).thenReturn(Date(now + 5000))

    mockMvc.perform(get("/rate-limited-shared")).andIsOk
    mockMvc.perform(get("/rate-limited-shared-2")).andIsOk
    mockMvc.perform(get("/rate-limited-shared")).andIsRateLimited
    mockMvc.perform(get("/rate-limited-shared-2")).andIsRateLimited
  }

  @Test
  fun `it does not rate limit when limits are disabled`() {
    Mockito.`when`(rateLimitProperties.endpointLimits).thenReturn(false)
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsOk
    mockMvc.perform(get("/rate-limited")).andIsOk

    Mockito.verify(rateLimitService, Mockito.never()).consumeBucket(any())

    mockMvc.perform(get("/rate-limited-auth")).andIsOk
    mockMvc.perform(get("/rate-limited-auth")).andIsOk
    mockMvc.perform(get("/rate-limited-auth")).andIsRateLimited
    mockMvc.perform(get("/rate-limited-auth")).andIsRateLimited

    Mockito.verify(rateLimitService, Mockito.times(4)).consumeBucket(any())

    Mockito.`when`(rateLimitProperties.endpointLimits).thenReturn(true)
    Mockito.`when`(rateLimitProperties.authenticationLimits).thenReturn(false)

    mockMvc.perform(get("/rate-limited-auth")).andIsOk

    Mockito.verify(rateLimitService, Mockito.times(4)).consumeBucket(any())
  }

  // Validate extraction
  @Test
  fun `endpoint rate limit policy is correctly extracted from annotations`() {
    val fakeRequest = makeFakeGenericRequest()

    val handlerWithoutLimit = HandlerMethod("", FakeController::fakeHandlerWithoutRateLimit.javaMethod!!)
    val handlerWithoutBucket = HandlerMethod("", FakeController::fakeHandler.javaMethod!!)
    val handlerWithBucket = HandlerMethod("", FakeController::fakeHandlerWithExplicitBucket.javaMethod!!)
    val handlerInherit = HandlerMethod("", FakeController::fakeHandlerInherit.javaMethod!!)

    val noPolicy = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest, null, handlerWithoutLimit)
    val policy1 = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest, null, handlerWithoutBucket)
    val policy2 = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest, null, handlerWithBucket)
    val policy3 = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest, null, handlerInherit)

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
  fun `endpoint rate limit bucket correctly discriminates against major path variables`() {
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

    val policyWithMajor1 = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest1, null, handlerWithMajor)
    val policyWithMajor2 = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest2, null, handlerWithMajor)
    val policyWithMajor3 = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest3, null, handlerWithMajor)

    val policyWithoutMajor1 = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest1, null, handlerWithoutMajor)
    val policyWithoutMajor2 = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest2, null, handlerWithoutMajor)

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

    val policy1 = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest, null, handler)
    val policy2 = rateLimitInterceptor.extractEndpointRateLimit(fakeRequest, userAccount, handler)

    assertThat(policy1).isNotNull
    assertThat(policy2).isNotNull

    assertThat(policy1?.bucketName).isEqualTo("endpoint.ip.127.0.0.1.GET /fake/route")
    assertThat(policy2?.bucketName).isEqualTo("endpoint.user.1337.GET /fake/route")
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
  }

  @RateLimited(2)
  annotation class InheritedRateLimit

  @RestController
  class TestController {
    @GetMapping("/")
    fun noRateLimit(): String {
      return "henlo!"
    }

    @GetMapping("/rate-limited")
    @RateLimited(2)
    fun rateLimited(): String {
      return "henlo!"
    }

    @GetMapping("/rate-limited-2")
    @RateLimited(2)
    fun rateLimited2(): String {
      return "henlo!"
    }

    @GetMapping("/rate-limited-auth")
    @RateLimited(2, isAuthentication = true)
    fun rateLimitedAuth(): String {
      return "henlo!"
    }

    @GetMapping("/rate-limited-shared")
    @RateLimited(2, bucketName = "shared")
    fun rateLimitedShared1(): String {
      return "henlo!"
    }

    @GetMapping("/rate-limited-shared-2")
    @RateLimited(2, bucketName = "shared")
    fun rateLimitedShared2(): String {
      return "henlo!"
    }
  }

  class FakeController {
    @RateLimited(2)
    fun fakeHandler() {
    }

    @RateLimited(2, bucketName = "uwu")
    fun fakeHandlerWithExplicitBucket() {
    }

    @RateLimited(2, pathVariablesToDiscriminate = 0)
    fun fakeHandlerWithoutMajorDiscriminator() {
    }

    fun fakeHandlerWithoutRateLimit() {}

    @InheritedRateLimit
    fun fakeHandlerInherit() {
    }
  }
}
