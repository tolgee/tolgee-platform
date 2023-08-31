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

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.security.authentication.AuthenticationFacade
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class GlobalUserRateLimitFilterTest {
  private val rateLimitService = Mockito.mock(RateLimitService::class.java)

  private val authenticationFacade = Mockito.mock(AuthenticationFacade::class.java)

  private val userAccount = Mockito.mock(UserAccountDto::class.java)

  private val rateLimitFilter = GlobalUserRateLimitFilter(rateLimitService, authenticationFacade)

  @BeforeEach
  fun setupMocks() {
    Mockito.`when`(rateLimitService.getGlobalUserRateLimitPolicy(any(), any()))
      .thenReturn(
        RateLimitPolicy("uwu", 5, 1000, false)
      )

    Mockito.`when`(authenticationFacade.isAuthenticated).thenReturn(true)
    Mockito.`when`(authenticationFacade.authenticatedUser).thenReturn(userAccount)
    Mockito.`when`(userAccount.id).thenReturn(1337L)
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(rateLimitService, authenticationFacade)
  }

  @Test
  fun `it lets requests through`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    assertDoesNotThrow { rateLimitFilter.doFilter(req, res, chain) }

    Mockito.verify(rateLimitService, Mockito.atLeastOnce()).getGlobalUserRateLimitPolicy(req, userAccount.id)
    Mockito.verify(rateLimitService, Mockito.atLeastOnce()).consumeBucket(any())
  }

  @Test
  fun `it does not let rate limited requests through`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    Mockito.`when`(rateLimitService.consumeBucket(any())).thenThrow(RateLimitedException(1000, true))
    assertThrows<RateLimitedException> { rateLimitFilter.doFilter(req, res, chain) }
  }

  @Test
  fun `it does not apply rate limiting when there are no policy defined`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    Mockito.`when`(rateLimitService.getGlobalUserRateLimitPolicy(any(), eq(1337L))).thenReturn(null)
    assertDoesNotThrow { rateLimitFilter.doFilter(req, res, chain) }

    Mockito.verify(rateLimitService, Mockito.never()).consumeBucket(any())
  }
}
