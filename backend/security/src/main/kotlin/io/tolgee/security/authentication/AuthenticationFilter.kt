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
import io.tolgee.component.KeyGenerator
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthExpiredException
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.PermissionException
import io.tolgee.security.BILLING_API_KEY_PREFIX
import io.tolgee.security.PAT_PREFIX
import io.tolgee.security.ratelimit.RateLimitService
import io.tolgee.security.thirdParty.SsoDelegate
import io.tolgee.service.apps.AppEnablementService
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.PatService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Lazy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Base64

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
  private val appEnablementService: AppEnablementService,
  @Lazy
  private val userAccountService: UserAccountService,
  @Lazy
  private val apiKeyService: ApiKeyService,
  @Lazy
  private val patService: PatService,
  @Lazy
  private val ssoDelegate: SsoDelegate,
  @Lazy
  private val keyGenerator: KeyGenerator,
  @Lazy
  private val permissionService: PermissionService,
) : OncePerRequestFilter() {
  companion object {
    private const val APP_SECRET_PREFIX = "tgapps_"
    private const val APP_CLIENT_ID_PREFIX = "tgapp_"
    private const val ACTING_AS_USER_HEADER = "X-Tolgee-Act-As-User-Id"
    private val PROJECT_URL_REGEX = Regex("/v2/projects/(\\d+)")
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

        // Try app-token (audience `tg.app`) first. If validation throws because the token
        // is not an app token (wrong audience), fall back to the user JWT path. Any other
        // failure (bad signature, expired, missing entities) surfaces as an auth error
        // and we do not fall through.
        val appAuth = tryAppTokenAuth(token)
        if (appAuth != null) {
          checkIfSsoUserStillValid(appAuth.principal)
          SecurityContextHolder.getContext().authentication = appAuth
          return
        }

        val auth = jwtService.validateToken(token)
        checkIfSsoUserStillValid(auth.principal)

        SecurityContextHolder.getContext().authentication = auth
        return
      }

      if (authorization.startsWith("Basic ")) {
        appBasicAuth(request, authorization.substring(6))
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

      if (apiKey.startsWith(APP_SECRET_PREFIX)) {
        appSecretAuth(request, apiKey)
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
   * Returns an [AppAuthentication] if the token parses as an app token and the live
   * entity resolution (install + user + tokensValidNotBefore + per-project enablement)
   * succeeds. Returns null when the token is not an app token (so the caller falls
   * back to user JWT validation). Throws [AuthenticationException] for app tokens that
   * are well-formed but reference revoked or missing entities.
   */
  private fun tryAppTokenAuth(token: String): AppAuthentication? {
    val claims =
      try {
        appTokenService.validateToken(token)
      } catch (_: AuthenticationException) {
        // wrong audience, bad signature, expired or malformed — let the caller try user JWT
        return null
      }

    val install =
      appInstallService.find(claims.installId)
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    val user =
      userAccountService.findDto(claims.userId)
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    if (user.tokensValidNotBefore != null && claims.issuedAt.before(user.tokensValidNotBefore)) {
      throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
    }

    if (!appEnablementService.isEnabledForProject(claims.projectId, claims.installId)) {
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }

    return AppAuthentication(
      credentials = token,
      appInstall = install,
      userAccount = user,
      projectId = claims.projectId,
      isInstallContext = false,
    )
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

  private fun appSecretAuth(
    request: HttpServletRequest,
    key: String,
  ) {
    val resolution =
      appInstallService.resolveByClientSecretHash(keyGenerator.hash(key))
        ?: throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
    populateAppAuth(request, resolution, credentials = key)
  }

  private fun appBasicAuth(
    request: HttpServletRequest,
    encoded: String,
  ) {
    val decoded =
      try {
        String(Base64.getDecoder().decode(encoded))
      } catch (_: IllegalArgumentException) {
        throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
      }
    val separator = decoded.indexOf(':')
    if (separator < 0) {
      throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
    }
    val clientId = decoded.substring(0, separator)
    val clientSecret = decoded.substring(separator + 1)
    if (!clientId.startsWith(APP_CLIENT_ID_PREFIX) || !clientSecret.startsWith(APP_SECRET_PREFIX)) {
      throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
    }

    val resolution =
      appInstallService.resolveByClientId(clientId)
        ?: throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
    val providedHash = keyGenerator.hash(clientSecret)
    val storedHash = resolution.install.clientSecretHash
    if (storedHash == null || !constantTimeEquals(providedHash, storedHash)) {
      throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
    }
    populateAppAuth(request, resolution, credentials = clientId)
  }

  private fun populateAppAuth(
    request: HttpServletRequest,
    resolution: AppInstallService.AppCredentialResolution,
    credentials: Any?,
  ) {
    val install = resolution.install
    val projectId = extractProjectIdFromUrl(request)
    val actingAsUser = resolveActingAsUser(request, projectId)

    if (projectId != null && !appEnablementService.isEnabledForProject(projectId, install.id)) {
      throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
    }

    SecurityContextHolder.getContext().authentication =
      AppAuthentication(
        credentials = credentials,
        userAccount = resolution.authorPrincipal,
        appInstall = install,
        projectId = projectId,
        isInstallContext = true,
        actingAsUserAccount = actingAsUser,
      )
  }

  private fun resolveActingAsUser(
    request: HttpServletRequest,
    projectId: Long?,
  ): UserAccountDto? {
    val raw = request.getHeader(ACTING_AS_USER_HEADER) ?: return null
    // Acting-as is only meaningful (and only scope-checked) within a project context. Without a
    // project in the URL there is nothing to bound the acted-as user's permissions against, so we
    // refuse rather than impersonate an arbitrary user on org/user-level endpoints.
    if (projectId == null) {
      throw PermissionException(Message.APP_ACTING_AS_USER_NOT_PROJECT_MEMBER)
    }
    val userId =
      raw.toLongOrNull() ?: throw AuthenticationException(Message.INVALID_APP_CREDENTIALS)
    val user =
      userAccountService.findDto(userId)
        ?: throw PermissionException(Message.APP_ACTING_AS_USER_NOT_PROJECT_MEMBER)
    val scopes = permissionService.getProjectPermissionScopesNoApiKey(projectId, user.id)
    if (scopes.isNullOrEmpty()) {
      throw PermissionException(Message.APP_ACTING_AS_USER_NOT_PROJECT_MEMBER)
    }
    return user
  }

  private fun extractProjectIdFromUrl(request: HttpServletRequest): Long? {
    val match = PROJECT_URL_REGEX.find(request.requestURI) ?: return null
    return match.groupValues.getOrNull(1)?.toLongOrNull()
  }

  private fun constantTimeEquals(
    a: String,
    b: String,
  ): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
      result = result or (a[i].code xor b[i].code)
    }
    return result == 0
  }

  private val initialUser by lazy {
    val account =
      userAccountService.findInitialUser()
        ?: throw IllegalStateException("Initial user does not exists")
    UserAccountDto.fromEntity(account)
  }
}
