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

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.AuthExpiredException
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.apps.App
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.toWholeSeconds
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class AppTokenAuthenticator(
  @Lazy
  private val appTokenService: AppTokenService,
  @Lazy
  private val appInstallService: AppInstallService,
  @Lazy
  private val userAccountService: UserAccountService,
  private val tolgeeProperties: TolgeeProperties,
) {
  fun authenticate(
    request: HttpServletRequest,
    token: String,
  ): AppAuthentication? {
    if (!tolgeeProperties.apps.enabled) return null
    // The prefix marks the token as an app token, so a malformed one fails here rather than falling
    // through to JWT auth as if it were a user token.
    if (!appTokenService.isAppToken(token)) return null

    val claims = appTokenService.validateToken(token)

    if (claims.isAppContext) {
      return appLevelAuth(token, claims)
    }
    if (claims.isInstallContext) {
      return installContextAuth(request, token, claims)
    }
    return userContextAuth(request, token, claims)
  }

  private fun appLevelAuth(
    token: String,
    claims: AppTokenClaims,
  ): AppAuthentication {
    val app =
      appInstallService.findAppForAppAuth(claims.appId!!)
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    checkAppTokenCutoff(app, claims)

    return AppAuthentication.appLevel(
      credentials = token,
      appId = app.id,
      isReadOnly = claims.isReadOnly,
    )
  }

  private fun userContextAuth(
    request: HttpServletRequest,
    token: String,
    claims: AppTokenClaims,
  ): AppAuthentication {
    if (request.getHeader(AuthenticationFilter.ACTING_AS_USER_HEADER) != null) {
      throw AuthenticationException(Message.APP_INVALID_ACTING_AS_USER_ID)
    }

    val install =
      appInstallService.findForAppAuth(claims.installId!!)
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    checkAppTokenCutoff(install.app, claims)

    val user = resolveAppTokenUser(claims.userId!!, claims)

    return AppAuthentication(
      credentials = token,
      appInstallOrNull = install,
      appId = install.app.id,
      userAccount = user,
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

    checkAppTokenCutoff(resolution.install.app, claims)

    return AppAuthentication(
      credentials = token,
      appInstallOrNull = resolution.install,
      appId = resolution.install.app.id,
      userAccount = resolution.principal,
      isInstallContext = true,
      isReadOnly = claims.isReadOnly,
      actsForUserId = resolveActingAsUserId(request),
    )
  }

  private fun checkAppTokenCutoff(
    app: App,
    claims: AppTokenClaims,
  ) {
    val cutoff = app.tokensInvalidBefore ?: return
    // Compare at whole seconds: a JWT `iat` is second-precision, so a token minted in the same second
    // as the cutoff would otherwise read as older than it and be wrongly rejected.
    if (claims.issuedAt.toWholeSeconds() < cutoff.toWholeSeconds()) {
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

  private fun resolveActingAsUserId(request: HttpServletRequest): Long? {
    val raw = request.getHeader(AuthenticationFilter.ACTING_AS_USER_HEADER) ?: return null
    return raw.toLongOrNull() ?: throw AuthenticationException(Message.APP_INVALID_ACTING_AS_USER_ID)
  }
}
