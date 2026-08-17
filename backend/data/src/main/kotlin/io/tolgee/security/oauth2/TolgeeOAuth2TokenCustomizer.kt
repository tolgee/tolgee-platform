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

  // A refresh-minted access token carries a fresh iat, so it slips the resolver's tokensValidNotBefore check; gate the
  // refresh grant here instead.
  private fun rejectRefreshOfInvalidatedTokens(context: JwtEncodingContext) {
    if (context.authorizationGrantType != AuthorizationGrantType.REFRESH_TOKEN) return
    val authorization = context.getAuthorization() ?: return
    val userId = authorization.principalName?.toLongOrNull() ?: return
    val user = userAccountService.findDto(userId)
    if (user == null || user.isTokenInvalidated(authorization.refreshToken?.token?.issuedAt)) {
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
    context.getAuthorization()?.projectHint()?.let { return listOf(it.toString()) }
    // A consent-required client reaching here ran no select-project and sent no project hint — SAS skipped the consent
    // screen because consent is remembered. Fail closed rather than silently widening from the consented project to
    // ALL_PROJECTS; the client must re-prompt for consent (or send a project hint) to re-establish the binding.
    if (context.registeredClient.clientSettings.isRequireAuthorizationConsent) {
      throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST)
    }
    return OAuth2Constants.ALL_PROJECTS
  }

  private fun projectSetFor(selection: String): Any {
    if (selection == OAuth2Constants.ALL_PROJECTS) return OAuth2Constants.ALL_PROJECTS
    return listOf(selection)
  }

  private fun consentSelection(context: JwtEncodingContext): String? =
    context.getAuthorization()?.getAttribute<String>(OAuth2Constants.PROJECT_ATTRIBUTE)
}
