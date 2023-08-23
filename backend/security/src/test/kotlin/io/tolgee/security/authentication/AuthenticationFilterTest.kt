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

package io.tolgee.security.authentication

import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.security.ratelimit.RateLimitPolicy
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.security.ratelimit.RateLimitedException
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class AuthenticationFilterTest {
  companion object {
    const val TEST_VALID_TOKEN = "uwu"
    const val TEST_INVALID_TOKEN = "owo"
    const val TEST_USER_ID = 1337L
  }

  private val authProperties = Mockito.mock(AuthenticationProperties::class.java)

  private val rateLimitService = Mockito.mock(RateLimitService::class.java)

  private val jwtService = Mockito.mock(JwtService::class.java)

  private val userAccount = Mockito.mock(UserAccount::class.java)

  private val authenticationFilter = AuthenticationFilter(authProperties, rateLimitService, jwtService)

  @BeforeEach
  fun setupMocksAndSecurityCtx() {
    Mockito.`when`(authProperties.enabled).thenReturn(true)

    Mockito.`when`(rateLimitService.getIpAuthRateLimitPolicy(any()))
      .thenReturn(
        RateLimitPolicy("test policy", 5, 1000, true)
      )

    Mockito.`when`(rateLimitService.consumeBucketUnless(any(), any()))
      .then {
        val fn = it.getArgument<() -> Boolean>(1)
        fn()
      }

    Mockito.`when`(jwtService.validateToken(TEST_VALID_TOKEN))
      .thenReturn(
        TolgeeAuthentication(
          "uwu",
          userAccount,
          null,
        )
      )

    Mockito.`when`(jwtService.validateToken(TEST_INVALID_TOKEN))
      .thenThrow(AuthenticationException(Message.INVALID_JWT_TOKEN))

    Mockito.`when`(userAccount.id).thenReturn(TEST_USER_ID)

    SecurityContextHolder.getContext().authentication = null
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(authProperties, rateLimitService, jwtService)
  }

  @Test
  fun `it does not filter when auth is disabled`() {
    Mockito.`when`(authProperties.enabled).thenReturn(false)
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    assertDoesNotThrow { authenticationFilter.doFilter(req, res, chain) }

    val ctx = SecurityContextHolder.getContext()
    assertThat(ctx.authentication).isNull()
  }

  @Test
  fun `it allows request to go through with valid JWT token`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    req.addHeader("Authorization", "Bearer $TEST_VALID_TOKEN")
    assertDoesNotThrow { authenticationFilter.doFilter(req, res, chain) }

    val ctx = SecurityContextHolder.getContext()
    assertThat(ctx.authentication).isNotNull
  }

  @Test
  fun `it does not allow request to go through with invalid JWT tokens`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    req.addHeader("Authorization", "Bearer $TEST_INVALID_TOKEN")
    assertThrows<AuthenticationException> { authenticationFilter.doFilter(req, res, chain) }

    chain.reset()
    req.removeHeader("Authorization")
    req.addHeader("Authorization", TEST_VALID_TOKEN)
    assertThrows<AuthenticationException> { authenticationFilter.doFilter(req, res, chain) }
  }

  @Test
  fun `it applies a rate limit on authentication attempts`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    Mockito.`when`(rateLimitService.consumeBucketUnless(any(), any()))
      .thenThrow(RateLimitedException(1000L, true))

    req.addHeader("Authorization", "Bearer $TEST_VALID_TOKEN")
    assertThrows<RateLimitedException> { authenticationFilter.doFilter(req, res, chain) }
  }
}
