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
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthExpiredException
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.security.BILLING_API_KEY_PREFIX
import io.tolgee.security.PAT_PREFIX
import io.tolgee.security.oauth2.OAuth2AccessTokenResolver
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.security.thirdParty.SsoDelegate
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.PatService
import io.tolgee.service.security.UserAccountService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Lazy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Lazy
class AuthenticationFilter(
  private val tolgeeProperties: TolgeeProperties,
  @Lazy
  private val currentDateProvider: CurrentDateProvider,
  @Lazy
  private val rateLimitService: RateLimitService,
  @Lazy
  private val jwtService: JwtService,
  @Lazy
  private val oauth2AccessTokenResolver: OAuth2AccessTokenResolver,
  @Lazy
  private val userAccountService: UserAccountService,
  @Lazy
  private val apiKeyService: ApiKeyService,
  @Lazy
  private val patService: PatService,
  @Lazy
  private val ssoDelegate: SsoDelegate,
) : OncePerRequestFilter() {
  private val authenticationProperties
    get() = tolgeeProperties.authentication
  private val internalProperties
    get() = tolgeeProperties.internal

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    val policy = rateLimitService.getIpAuthRateLimitPolicy(request)

    if (policy == null) {
      doAuthenticate(request)
    } else {
      rateLimitService.consumeBucketUnless(policy) {
        doAuthenticate(request)
        true
      }
    }

    filterChain.doFilter(request, response)
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    return request.method == "OPTIONS"
  }

  private fun doAuthenticate(request: HttpServletRequest) {
    // The authorization server authenticates nobody: /oauth2/token and /oauth2/revoke identify their caller by the
    // grant they present, and discovery is public. Resolving a credential here would let a stale Authorization header
    // 401 the very requests a client makes to recover — including the RFC 9728 document a 401 pointed it at. Only the
    // credential resolution is skipped: the filter itself still runs, so these paths keep the per-IP auth rate limit.
    if (request.requestURI.removePrefix(request.contextPath) in AUTHORIZATION_SERVER_PATHS) return

    val authorization = request.getHeader("Authorization")
    if (authorization != null) {
      if (authorization.startsWith("Bearer ")) {
        val token = authorization.substring(7)
        val auth = oauth2AccessTokenResolver.tryResolve(token) ?: jwtService.validateToken(token)
        checkIfSsoUserStillValid(auth.principal)

        SecurityContextHolder.getContext().authentication = auth
        return
      }

      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }

    val apiKey = request.getHeader("X-API-Key") ?: request.getParameter("ak")
    if (apiKey != null) {
      if (apiKey.startsWith(BILLING_API_KEY_PREFIX)) {
        return // Skip - handled by billing stats controller
      }

      if (apiKey.startsWith(PAT_PREFIX)) {
        patAuth(apiKey)
        return
      }

      // Attempt PAK auth even if it doesn't have the prefix
      // Might be a legacy key
      pakAuth(apiKey)
      return
    }

    // even if the authentication is disabled, they still might be using PAK for in-context editing,
    // so we still need to try tho authenticate using API key, to have API key authentication in the security context
    if (!authenticationProperties.enabled) {
      SecurityContextHolder.getContext().authentication =
        TolgeeAuthentication(
          credentials = null,
          deviceId = null,
          userAccount = initialUser,
          actingAsUserAccount = null,
          isReadOnly = false,
          isSuperToken = true,
        )
    }
  }

  private fun checkIfSsoUserStillValid(userDto: UserAccountDto) {
    when (internalProperties.verifySsoAccountAvailableBypass) {
      true -> {
        // Bypass user validity check
        return
      }

      false -> {
        // Always fail user validity check
        throw AuthExpiredException(Message.SSO_CANT_VERIFY_USER)
      }

      null -> {
        if (!ssoDelegate.verifyUserSsoAccountAvailable(userDto)) {
          throw AuthExpiredException(Message.SSO_CANT_VERIFY_USER)
        }
      }
    }
  }

  private fun pakAuth(key: String) {
    val parsed =
      apiKeyService.parseApiKey(key)
        ?: throw AuthenticationException(Message.INVALID_PROJECT_API_KEY)

    val hash = apiKeyService.hashKey(parsed)
    val pak =
      apiKeyService.findDto(hash)
        ?: throw AuthenticationException(Message.INVALID_PROJECT_API_KEY)

    if (pak.expiresAt?.before(currentDateProvider.date) == true) {
      throw AuthenticationException(Message.PROJECT_API_KEY_EXPIRED)
    }

    val userAccount =
      userAccountService.findDto(pak.userAccountId)
        ?: throw AuthenticationException(Message.USER_NOT_FOUND)

    checkIfSsoUserStillValid(userAccount)

    apiKeyService.updateLastUsedAsync(pak.id)
    SecurityContextHolder.getContext().authentication =
      TolgeeAuthentication(
        credentials = pak,
        deviceId = null,
        userAccount = userAccount,
        actingAsUserAccount = null,
        isReadOnly = false,
        isSuperToken = false,
      )
  }

  private fun patAuth(key: String) {
    val hash = patService.hashToken(key.substring(PAT_PREFIX.length))
    val pat =
      patService.findDto(hash)
        ?: throw AuthenticationException(Message.INVALID_PAT)

    if (pat.expiresAt?.before(currentDateProvider.date) == true) {
      throw AuthenticationException(Message.PAT_EXPIRED)
    }

    val userAccount =
      userAccountService.findDto(pat.userAccountId)
        ?: throw AuthenticationException(Message.USER_NOT_FOUND)

    checkIfSsoUserStillValid(userAccount)

    patService.updateLastUsedAsync(pat.id)
    SecurityContextHolder.getContext().authentication =
      TolgeeAuthentication(
        credentials = pat,
        deviceId = null,
        userAccount = userAccount,
        actingAsUserAccount = null,
        isReadOnly = false,
        isSuperToken = false,
      )
  }

  private val initialUser by lazy {
    val account =
      userAccountService.findInitialUser()
        ?: throw IllegalStateException("Initial user does not exists")
    UserAccountDto.fromEntity(account)
  }

  companion object {
    private val AUTHORIZATION_SERVER_PATHS =
      setOf(
        OAuth2Constants.AUTHORIZE_PATH,
        OAuth2Constants.TOKEN_PATH,
        OAuth2Constants.REVOKE_PATH,
        OAuth2Constants.AUTHORIZATION_SERVER_METADATA_PATH,
        OAuth2Constants.PROTECTED_RESOURCE_METADATA_PATH,
      )
  }
}
