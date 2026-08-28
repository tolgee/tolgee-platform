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
import io.tolgee.model.apps.AppInstall
import io.tolgee.service.apps.AppInstallService
import io.tolgee.service.security.UserAccountService
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

    val claims =
      try {
        appTokenService.validateToken(token)
      } catch (e: AuthExpiredException) {
        throw e
      } catch (_: AuthenticationException) {
        return null
      }

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

    app.tokensInvalidBefore?.let { cutoff ->
      if (claims.issuedAt.before(cutoff)) {
        throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
      }
    }

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

    assertNotRevokedByAppCutoff(install, claims)

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

    assertNotRevokedByAppCutoff(resolution.install, claims)

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

  private fun resolveActingAsUserId(request: HttpServletRequest): Long? {
    val raw = request.getHeader(AuthenticationFilter.ACTING_AS_USER_HEADER) ?: return null
    return raw.toLongOrNull() ?: throw AuthenticationException(Message.APP_INVALID_ACTING_AS_USER_ID)
  }
}
