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
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.security.PAT_PREFIX
import io.tolgee.security.ratelimit.RateLimitService
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
  private val authenticationProperties: AuthenticationProperties,
  @Lazy
  private val currentDateProvider: CurrentDateProvider,
  @Lazy
  private val rateLimitService: RateLimitService,
  @Lazy
  private val jwtService: JwtService,
  @Lazy
  private val userAccountService: UserAccountService,
  @Lazy
  private val apiKeyService: ApiKeyService,
  @Lazy
  private val patService: PatService,
) : OncePerRequestFilter() {
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
    val authorization = request.getHeader("Authorization")
    if (authorization != null) {
      if (authorization.startsWith("Bearer ")) {
        val auth = jwtService.validateToken(authorization.substring(7))
        SecurityContextHolder.getContext().authentication = auth
        return
      }

      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }

    val apiKey = request.getHeader("X-API-Key") ?: request.getParameter("ak")
    if (apiKey != null) {
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
          null,
          initialUser,
          TolgeeAuthenticationDetails(true),
        )
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

    apiKeyService.updateLastUsedAsync(pak.id)
    SecurityContextHolder.getContext().authentication =
      TolgeeAuthentication(
        pak,
        userAccount,
        TolgeeAuthenticationDetails(false),
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

    patService.updateLastUsedAsync(pat.id)
    SecurityContextHolder.getContext().authentication =
      TolgeeAuthentication(
        pat,
        userAccount,
        TolgeeAuthenticationDetails(false),
      )
  }

  private val initialUser by lazy {
    val account =
      userAccountService.findInitialUser()
        ?: throw IllegalStateException("Initial user does not exists")
    UserAccountDto.fromEntity(account)
  }
}
