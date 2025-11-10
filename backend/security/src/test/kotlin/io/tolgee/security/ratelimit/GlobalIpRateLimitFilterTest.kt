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

import io.tolgee.security.authentication.AuthenticationFacade
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class GlobalIpRateLimitFilterTest {
  private val rateLimitService = Mockito.mock(RateLimitService::class.java)

  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val rateLimitFilter = GlobalIpRateLimitFilter(rateLimitService, authenticationFacade)

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(authenticationFacade.isAuthenticated).thenReturn(false)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(rateLimitService)
  }

  @Test
  fun `it lets requests through`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    assertDoesNotThrow { rateLimitFilter.doFilter(req, res, chain) }

    Mockito.verify(rateLimitService, Mockito.atLeastOnce()).consumeGlobalIpRateLimitPolicy(req)
  }

  @Test
  fun `it does not let rate limited requests through`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    Mockito
      .`when`(rateLimitService.consumeGlobalIpRateLimitPolicy(any()))
      .thenThrow(RateLimitedException(1000, true))

    assertThrows<RateLimitedException> { rateLimitFilter.doFilter(req, res, chain) }
  }

  @Test
  fun `it does rate limit if request is OPTIONS`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()
    req.method = "OPTIONS"

    Mockito
      .`when`(rateLimitService.consumeGlobalIpRateLimitPolicy(any()))
      .thenThrow(RateLimitedException(1000, true))

    assertDoesNotThrow { rateLimitFilter.doFilter(req, res, chain) }
  }
}
