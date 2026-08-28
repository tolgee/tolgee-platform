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
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.stereotype.Component

@Component
class TolgeeOAuth2TokenCustomizer(
  private val userAccountService: UserAccountService,
  private val authorizationQueryService: OAuth2AuthorizationQueryService,
) : OAuth2TokenCustomizer<OAuth2TokenClaimsContext> {
  override fun customize(context: OAuth2TokenClaimsContext) {
    if (context.tokenType != OAuth2TokenType.ACCESS_TOKEN) return

    rejectRefreshOfInvalidatedTokens(context)
    rejectConsentedClientWithNothingSelected(context)
    context.claims.claim(OAuth2Constants.PROJECTS_CLAIM, projectClaim(context))
  }

  // A refresh-minted access token carries a fresh iat, so it slips the resolver's tokensValidNotBefore check; gate the
  // refresh grant here instead.
  private fun rejectRefreshOfInvalidatedTokens(context: OAuth2TokenClaimsContext) {
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

  /**
   * Defence in depth: with consent never remembered ([AlwaysPromptConsentService]) a consent-requiring client always
   * reaches here with a selection. Were one ever skipped, `project` is a request parameter the client chose, so
   * honouring it would bind the token to a project nobody was shown.
   */
  private fun rejectConsentedClientWithNothingSelected(context: OAuth2TokenClaimsContext) {
    if (consentSelection(context) != null) return
    if (!context.registeredClient.clientSettings.isRequireAuthorizationConsent) return
    throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST)
  }

  /**
   * Either the [OAuth2Constants.ALL_PROJECTS] sentinel or a list of project ids.
   *
   * For a client that asks for no consent there is no approved project set, so the token is as wide as the user —
   * which is what [OAuth2Constants.ALL_PROJECTS] means here. The `project` request parameter is then honoured because
   * it can only narrow that: it is intersected with live permissions like any other binding, so a client can restrict
   * its own token but never reach past the user it authenticates.
   */
  private fun projectClaim(context: OAuth2TokenClaimsContext): Any {
    consentSelection(context)?.let { return claimFor(it) }
    context.getAuthorization()?.projectHint()?.let { return claimFor(it.toString()) }
    return OAuth2Constants.ALL_PROJECTS
  }

  private fun claimFor(projectId: String): Any {
    if (projectId == OAuth2Constants.ALL_PROJECTS) return OAuth2Constants.ALL_PROJECTS
    // Ids are stamped as strings: SAS's JDBC polymorphic-type validator rejects java.lang.Long when it deserializes
    // the stored claims on the refresh grant, which would make a project-bound token unrefreshable.
    return listOf(projectId)
  }

  private fun consentSelection(context: OAuth2TokenClaimsContext): String? =
    context.getAuthorization()?.getAttribute<String>(OAuth2Constants.PROJECT_ATTRIBUTE)
}
