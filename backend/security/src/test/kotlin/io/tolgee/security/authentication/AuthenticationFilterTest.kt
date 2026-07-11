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

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.ApiKeyDto
import io.tolgee.dtos.cacheable.PatDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.UserAccount
import io.tolgee.security.ratelimit.RateLimitPolicy
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.security.ratelimit.RateLimitedException
import io.tolgee.security.thirdParty.SsoDelegate
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.PatService
import io.tolgee.service.security.UserAccountService
import io.tolgee.testing.assertions.Assertions.assertThat
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
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Duration
import java.util.Date

class AuthenticationFilterTest {
  companion object {
    const val TEST_VALID_TOKEN = "uwu"
    const val TEST_INVALID_TOKEN = "owo"
    const val TEST_USER_ID = 1337L

    const val TEST_VALID_PAK = "tgpak_valid"
    const val TEST_INVALID_PAK = "tgpak_invalid"

    const val TEST_VALID_PAT = "tgpat_valid"
    const val TEST_INVALID_PAT = "tgpat_invalid"
  }

  private val currentDateProvider = Mockito.mock(CurrentDateProvider::class.java)

  private val tolgeeProperties = Mockito.mock(TolgeeProperties::class.java)

  private val authProperties = Mockito.mock(AuthenticationProperties::class.java)

  private val internalProperties = Mockito.mock(InternalProperties::class.java)

  private val rateLimitService = Mockito.mock(RateLimitService::class.java)

  private val jwtService = Mockito.mock(JwtService::class.java)

  private val pakService = Mockito.mock(ApiKeyService::class.java)

  private val patService = Mockito.mock(PatService::class.java)

  private val userAccountService = Mockito.mock(UserAccountService::class.java)

  private val apiKey = Mockito.mock(ApiKeyDto::class.java)

  private val pat = Mockito.mock(PatDto::class.java)

  private val userAccountDto = Mockito.mock(UserAccountDto::class.java)

  private val userAccount = Mockito.mock(UserAccount::class.java, Mockito.RETURNS_DEFAULTS)

  private val ssoDelegate = Mockito.mock(SsoDelegate::class.java)

  private val authenticationFilter =
    AuthenticationFilter(
      tolgeeProperties,
      currentDateProvider,
      rateLimitService,
      jwtService,
      userAccountService,
      pakService,
      patService,
      ssoDelegate,
    )

  private val authenticationFacade =
    AuthenticationFacade(
      userAccountService,
      Mockito.mock(ApiKeyService::class.java),
      Mockito.mock(PatService::class.java),
    )

  @BeforeEach
  fun setupMocksAndSecurityCtx() {
    val now = Date()
    Mockito.`when`(currentDateProvider.date).thenReturn(now)

    Mockito.`when`(tolgeeProperties.authentication).thenReturn(authProperties)
    Mockito.`when`(tolgeeProperties.internal).thenReturn(internalProperties)
    Mockito.`when`(authProperties.enabled).thenReturn(true)
    Mockito.`when`(internalProperties.verifySsoAccountAvailableBypass).thenReturn(null)

    Mockito
      .`when`(rateLimitService.getIpAuthRateLimitPolicy(any()))
      .thenReturn(
        RateLimitPolicy("test policy", 5, Duration.ofSeconds(1), true),
      )

    Mockito
      .`when`(rateLimitService.consumeBucketUnless(any(), any()))
      .then {
        val fn = it.getArgument<() -> Boolean>(1)
        fn()
      }

    Mockito
      .`when`(jwtService.validateToken(TEST_VALID_TOKEN))
      .thenReturn(
        TolgeeAuthentication(
          credentials = "uwu",
          deviceId = null,
          userAccount = userAccountDto,
          actingAsUserAccount = null,
          isReadOnly = false,
          isSuperToken = false,
        ),
      )

    Mockito
      .`when`(jwtService.validateToken(TEST_INVALID_TOKEN))
      .thenThrow(AuthenticationException(Message.INVALID_JWT_TOKEN))

    Mockito.`when`(pakService.parseApiKey(TEST_VALID_PAK)).thenReturn(TEST_VALID_PAK)
    Mockito.`when`(pakService.parseApiKey(TEST_INVALID_PAK)).thenReturn(TEST_INVALID_PAK)
    Mockito.`when`(pakService.hashKey(TEST_VALID_PAK)).thenReturn(TEST_VALID_PAK)
    Mockito.`when`(pakService.hashKey(TEST_INVALID_PAK)).thenReturn(TEST_INVALID_PAK)
    Mockito.`when`(pakService.findDto(Mockito.anyString())).thenReturn(null)
    Mockito.`when`(pakService.findDto(TEST_VALID_PAK)).thenReturn(apiKey)

    Mockito.`when`(patService.hashToken("valid")).thenReturn(TEST_VALID_PAT)
    Mockito.`when`(patService.hashToken("invalid")).thenReturn(TEST_INVALID_PAT)
    Mockito.`when`(patService.findDto(Mockito.anyString())).thenReturn(null)
    Mockito.`when`(patService.findDto(TEST_VALID_PAT)).thenReturn(pat)

    Mockito.`when`(userAccountService.findActive(TEST_USER_ID)).thenReturn(userAccount)
    Mockito.`when`(userAccountService.findDto(TEST_USER_ID)).thenReturn(userAccountDto)
    Mockito.`when`(userAccountService.findInitialUser()).thenReturn(Mockito.mock(UserAccount::class.java))

    Mockito.`when`(apiKey.userAccountId).thenReturn(TEST_USER_ID)
    Mockito.`when`(apiKey.expiresAt).thenReturn(null)
    Mockito.`when`(pat.userAccountId).thenReturn(TEST_USER_ID)
    Mockito.`when`(pat.expiresAt).thenReturn(null)

    Mockito.`when`(userAccount.id).thenReturn(TEST_USER_ID)
    Mockito.`when`(userAccount.name).thenReturn("")
    Mockito.`when`(userAccount.username).thenReturn("")
    Mockito.`when`(userAccount.needsSuperJwt).thenReturn(false)
    Mockito.`when`(userAccountDto.id).thenReturn(TEST_USER_ID)

    Mockito.`when`(ssoDelegate.verifyUserSsoAccountAvailable(userAccountDto)).thenReturn(true)

    SecurityContextHolder.getContext().authentication = null
  }

  @AfterEach
  fun resetMocks() {
    Mockito.reset(
      currentDateProvider,
      tolgeeProperties,
      authProperties,
      internalProperties,
      rateLimitService,
      jwtService,
      pakService,
      patService,
      apiKey,
      pat,
      userAccount,
    )
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
    assertThat(authenticationFacade.authenticatedUser.id).isEqualTo(userAccount.id)
    assertThat(authenticationFacade.isApiAuthentication).isEqualTo(false)
    assertThat(authenticationFacade.isProjectApiKeyAuth).isEqualTo(false)
    assertThat(authenticationFacade.isPersonalAccessTokenAuth).isEqualTo(false)
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
  fun `it allows request to go through when using valid PAK`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    req.addHeader("X-API-Key", TEST_VALID_PAK)
    assertDoesNotThrow { authenticationFilter.doFilter(req, res, chain) }

    val ctx = SecurityContextHolder.getContext()
    assertThat(ctx.authentication).isNotNull
    assertThat(authenticationFacade.authenticatedUser.id).isEqualTo(userAccount.id)
    assertThat(authenticationFacade.isApiAuthentication).isEqualTo(true)
    assertThat(authenticationFacade.isProjectApiKeyAuth).isEqualTo(true)
    assertThat(authenticationFacade.isPersonalAccessTokenAuth).isEqualTo(false)
    assertThat(authenticationFacade.projectApiKey).isEqualTo(apiKey)
  }

  @Test
  fun `it allows request to go through when using invalid PAK`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    req.addHeader("X-API-Key", TEST_INVALID_PAK)
    assertThrows<AuthenticationException> { authenticationFilter.doFilter(req, res, chain) }
  }

  @Test
  fun `it allows request to go through when using expired PAK`() {
    val now = currentDateProvider.date
    Mockito.`when`(apiKey.expiresAt).thenReturn(Date(now.time - 10_000))

    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    req.addHeader("X-API-Key", TEST_VALID_PAK)
    assertThrows<AuthenticationException> { authenticationFilter.doFilter(req, res, chain) }
  }

  @Test
  fun `it allows request to go through when using valid PAT`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    req.addHeader("X-API-Key", TEST_VALID_PAT)
    assertDoesNotThrow { authenticationFilter.doFilter(req, res, chain) }

    val ctx = SecurityContextHolder.getContext()
    assertThat(ctx.authentication).isNotNull
    assertThat(authenticationFacade.authenticatedUser.id).isEqualTo(userAccount.id)
    assertThat(authenticationFacade.isApiAuthentication).isEqualTo(true)
    assertThat(authenticationFacade.isProjectApiKeyAuth).isEqualTo(false)
    assertThat(authenticationFacade.isPersonalAccessTokenAuth).isEqualTo(true)
    assertThat(authenticationFacade.personalAccessToken).isEqualTo(pat)
  }

  @Test
  fun `it allows request to go through when using invalid PAT`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    req.addHeader("X-API-Key", TEST_INVALID_PAT)
    assertThrows<AuthenticationException> { authenticationFilter.doFilter(req, res, chain) }
  }

  @Test
  fun `it allows request to go through when using expired PAT`() {
    val now = currentDateProvider.date
    Mockito.`when`(pat.expiresAt).thenReturn(Date(now.time - 10_000))

    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    req.addHeader("X-API-Key", TEST_VALID_PAT)
    assertThrows<AuthenticationException> { authenticationFilter.doFilter(req, res, chain) }
  }

  @Test
  fun `it applies a rate limit on authentication attempts`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()

    Mockito
      .`when`(rateLimitService.consumeBucketUnless(any(), any()))
      .thenThrow(RateLimitedException(1000L, true))

    req.addHeader("Authorization", "Bearer $TEST_VALID_TOKEN")
    assertThrows<RateLimitedException> { authenticationFilter.doFilter(req, res, chain) }

    req.removeHeader("Authorization")
    req.addHeader("X-API-Key", TEST_VALID_PAK)
    assertThrows<RateLimitedException> { authenticationFilter.doFilter(req, res, chain) }

    req.removeHeader("X-API-Key")
    req.addHeader("X-API-Key", TEST_VALID_PAT)
    assertThrows<RateLimitedException> { authenticationFilter.doFilter(req, res, chain) }
  }

  @Test
  fun `it does not filter when request is OPTIONS`() {
    val req = MockHttpServletRequest()
    val res = MockHttpServletResponse()
    val chain = MockFilterChain()
    req.method = "OPTIONS"

    assertDoesNotThrow { authenticationFilter.doFilter(req, res, chain) }

    val ctx = SecurityContextHolder.getContext()
    assertThat(ctx.authentication).isNull()
  }
}
