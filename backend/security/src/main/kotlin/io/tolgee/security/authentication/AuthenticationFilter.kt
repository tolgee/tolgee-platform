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
import io.tolgee.model.apps.AppInstall
import io.tolgee.security.BILLING_API_KEY_PREFIX
import io.tolgee.security.PAT_PREFIX
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.security.thirdParty.SsoDelegate
import io.tolgee.service.apps.AppInstallService
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
  private val appTokenService: AppTokenService,
  @Lazy
  private val appInstallService: AppInstallService,
  @Lazy
  private val userAccountService: UserAccountService,
  @Lazy
  private val apiKeyService: ApiKeyService,
  @Lazy
  private val patService: PatService,
  @Lazy
  private val ssoDelegate: SsoDelegate,
) : OncePerRequestFilter() {
  companion object {
    const val ACTING_AS_USER_HEADER = "X-Tolgee-Act-As-User-Id"
  }

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
    val authorization = request.getHeader("Authorization")
    if (authorization != null) {
      if (authorization.startsWith("Bearer ")) {
        val token = authorization.substring(7)

        // App token first; null means not an app token, fall through to the user JWT.
        val appAuth = tryAppTokenAuth(request, token)
        if (appAuth != null) {
          // Only a user-context token carries a real person; install and app-level principals are
          // synthetic and have no identity provider to verify against.
          if (!appAuth.isInstallContext && !appAuth.isAppLevel) {
            checkIfSsoUserStillValid(appAuth.principal)
          }
          SecurityContextHolder.getContext().authentication = appAuth
          return
        }

        val auth = jwtService.validateToken(token)
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

  /**
   * [AppAuthentication] for a valid app token, or null when it is not an app token (caller falls back
   * to the user JWT). Throws for a well-formed app token referencing a revoked/missing entity.
   */
  private fun tryAppTokenAuth(
    request: HttpServletRequest,
    token: String,
  ): AppAuthentication? {
    // Kill switch: disabling the feature must stop already-minted tokens, not just new ones.
    if (!tolgeeProperties.apps.enabled) return null

    val claims =
      try {
        appTokenService.validateToken(token)
      } catch (e: AuthExpiredException) {
        throw e
      } catch (_: AuthenticationException) {
        return null
      }

    if (claims.isAppContext) {
      return AppAuthentication.appLevel(
        credentials = token,
        appId = claims.appId!!,
        isReadOnly = claims.isReadOnly,
      )
    }
    if (claims.isInstallContext) {
      return installContextAuth(request, token, claims)
    }
    return userContextAuth(request, token, claims)
  }

  private fun userContextAuth(
    request: HttpServletRequest,
    token: String,
    claims: AppTokenClaims,
  ): AppAuthentication {
    // Acting-as is install-context only; a user-context token is already a specific person, so the
    // header must be rejected rather than silently ignored.
    if (request.getHeader(ACTING_AS_USER_HEADER) != null) {
      throw AuthenticationException(Message.APP_INVALID_ACTING_AS_USER_ID)
    }

    val install =
      appInstallService.findForAppAuth(claims.installId!!)
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    assertNotRevokedByAppCutoff(install, claims)

    val user = resolveAppTokenUser(claims.userId!!, claims)

    return AppAuthentication(
      credentials = token,
      appInstallOrNull = install,
      appId = install.app.id,
      userAccount = user,
      tokenProjectId = claims.projectId,
      isInstallContext = false,
      isReadOnly = claims.isReadOnly,
    )
  }

  private fun installContextAuth(
    request: HttpServletRequest,
    token: String,
    claims: AppTokenClaims,
  ): AppAuthentication {
    val resolution =
      appInstallService.resolveForAppAuth(claims.installId!!)
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    assertNotRevokedByAppCutoff(resolution.install, claims)

    return AppAuthentication(
      credentials = token,
      appInstallOrNull = resolution.install,
      appId = resolution.install.app.id,
      userAccount = resolution.principal,
      tokenProjectId = null,
      isInstallContext = true,
      isReadOnly = claims.isReadOnly,
      actsForUserId = resolveActingAsUserId(request),
    )
  }

  /** A force-revoke cutoff kills every earlier token, user-context ones included. */
  private fun assertNotRevokedByAppCutoff(
    install: AppInstall,
    claims: AppTokenClaims,
  ) {
    val cutoff = install.app.tokensInvalidBefore ?: return
    if (claims.issuedAt.before(cutoff)) {
      throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
    }
  }

  private fun resolveAppTokenUser(
    userId: Long,
    claims: AppTokenClaims,
  ): UserAccountDto {
    val user =
      userAccountService.findDto(userId)
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    if (user.tokensValidNotBefore != null && claims.issuedAt.before(user.tokensValidNotBefore)) {
      throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
    }

    return user
  }

  /**
   * Parses the acted-as user id only. Its existence and project membership are checked later, in
   * [io.tolgee.security.ProjectContextService] once the project is known — resolving it here would
   * turn a route that binds no project into a server-wide user-id existence oracle.
   */
  private fun resolveActingAsUserId(request: HttpServletRequest): Long? {
    val raw = request.getHeader(ACTING_AS_USER_HEADER) ?: return null
    return raw.toLongOrNull() ?: throw AuthenticationException(Message.APP_INVALID_ACTING_AS_USER_ID)
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
}
