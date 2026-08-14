/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
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

package io.tolgee.security.oauth2

import io.tolgee.dtos.cacheable.isTokenInvalidated
import io.tolgee.service.security.UserAccountService
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.stereotype.Component

/**
 * Adds Tolgee's `tg.prj` (project set) and `aud` claims to OAuth2 access tokens. `sub` is SAS's default — the
 * authorization principal's name, which the session bootstrap sets to the numeric user id.
 */
@Component
class TolgeeOAuth2TokenCustomizer(
  private val audienceResolver: OAuth2AudienceResolver,
  private val userAccountService: UserAccountService,
  private val authorizationQueryService: OAuth2AuthorizationQueryService,
) : OAuth2TokenCustomizer<JwtEncodingContext> {
  override fun customize(context: JwtEncodingContext) {
    if (context.tokenType != OAuth2TokenType.ACCESS_TOKEN) return

    rejectRefreshOfInvalidatedTokens(context)
    context.claims.claim(OAuth2Constants.PROJECTS_CLAIM, projectSet(context))
    context.getAuthorization()?.id?.let { context.claims.claim(OAuth2Constants.AUTHORIZATION_ID_CLAIM, it) }
    context.claims.audience(listOf(audienceResolver.apiAudience))
  }

  // A refresh-minted access token carries a fresh iat, so it slips past the resolver's tokensValidNotBefore check —
  // gate the refresh grant itself instead, and revoke the dead grant so it can't be retried and its access tokens die.
  private fun rejectRefreshOfInvalidatedTokens(context: JwtEncodingContext) {
    if (context.authorizationGrantType != AuthorizationGrantType.REFRESH_TOKEN) return
    val authorization = context.getAuthorization() ?: return
    val userId = authorization.principalName?.toLongOrNull() ?: return
    val user = userAccountService.findDto(userId)
    if (user == null) {
      revokeAuthorization(authorization)
      throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT)
    }
    if (user.isTokenInvalidated(authorization.refreshToken?.token?.issuedAt)) {
      revokeAuthorization(authorization)
      throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_GRANT)
    }
  }

  private fun revokeAuthorization(authorization: OAuth2Authorization) {
    authorizationQueryService.revokeByIdInNewTransaction(authorization.id)
  }

  private fun projectSet(context: JwtEncodingContext): Any {
    // Stamp ids as strings: SAS's JDBC polymorphic-type validator rejects java.lang.Long when it deserializes the
    // stored claims on the refresh grant, which would otherwise make a project-bound token unrefreshable.
    consentSelection(context)?.let { return projectSetFor(it) }
    projectHint(context)?.let { return listOf(it.toString()) }
    return OAuth2Constants.ALL_PROJECTS
  }

  private fun projectSetFor(selection: String): Any {
    if (selection == OAuth2Constants.ALL_PROJECTS) return OAuth2Constants.ALL_PROJECTS
    return listOf(selection)
  }

  private fun consentSelection(context: JwtEncodingContext): String? =
    context.getAuthorization()?.getAttribute<String>(OAuth2Constants.PROJECT_ATTRIBUTE)

  private fun projectHint(context: JwtEncodingContext): Long? = context.getAuthorization()?.projectHint()
}
