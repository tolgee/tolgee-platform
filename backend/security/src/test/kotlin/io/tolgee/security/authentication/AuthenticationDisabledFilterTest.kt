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

import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.model.UserAccount
import io.tolgee.service.security.UserAccountService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.mock
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class AuthenticationDisabledFilterTest {
  companion object {
    const val TEST_USER_ID = 1337L
    const val TEST_INITIAL_USER_NAME = "admin"
  }

  private val tolgeeProperties = mock(TolgeeProperties::class.java)

  private val authProperties = mock(AuthenticationProperties::class.java)

  private val internalProperties = mock(InternalProperties::class.java)

  private val userAccountService = mock(UserAccountService::class.java)

  private val userAccount = mock(UserAccount::class.java)

  private val authenticationDisabledFilter =
    AuthenticationFilter(tolgeeProperties, mock(), mock(), mock(), userAccountService, mock(), mock(), mock())

  @BeforeEach
  fun setupMocksAndSecurityCtx() {
    Mockito.`when`(tolgeeProperties.authentication).thenReturn(authProperties)
    Mockito.`when`(tolgeeProperties.internal).thenReturn(internalProperties)
    Mockito.`when`(authProperties.enabled).thenReturn(false)
    Mockito.`when`(authProperties.initialUsername).thenReturn("admin")
    Mockito.`when`(internalProperties.verifySsoAccountAvailableBypass).thenReturn(null)

    Mockito.`when`(userAccountService.findInitialUser()).thenReturn(userAccount)

    Mockito.`when`(userAccount.id).thenReturn(TEST_USER_ID)
    Mockito.`when`(userAccount.name).thenReturn("")
    Mockito.`when`(userAccount.username).thenReturn("")
    Mockito.`when`(userAccount.needsSuperJwt).thenReturn(false)
    Mockito.`when`(userAccount.username).thenReturn(TEST_INITIAL_USER_NAME)

    SecurityContextHolder.getContext().authentication = null
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(authProperties, userAccountService)
  }

  @Test
  fun `it does not require authentication to go through`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    assertDoesNotThrow { authenticationDisabledFilter.doFilter(req, res, chain) }

    val ctx = SecurityContextHolder.getContext()
    assertThat(ctx.authentication).isNotNull

    val auth = ctx.authentication as TolgeeAuthentication
    assertThat(auth.principal.id).isEqualTo(TEST_USER_ID)
    assertThat(auth.principal.username).isEqualTo(TEST_INITIAL_USER_NAME)
  }

  @Test
  fun `it does not authenticate when authentication is enabled`() {
    Mockito.`when`(authProperties.enabled).thenReturn(true)

    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    assertDoesNotThrow { authenticationDisabledFilter.doFilter(req, res, chain) }

    val ctx = SecurityContextHolder.getContext()
    assertThat(ctx.authentication).isNull()
  }

  @Test
  fun `it does not filter when request is OPTIONS`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()
    req.method = "OPTIONS"

    assertDoesNotThrow { authenticationDisabledFilter.doFilter(req, res, chain) }

    val ctx = SecurityContextHolder.getContext()
    assertThat(ctx.authentication).isNull()
  }
}
