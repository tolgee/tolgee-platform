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

import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.isTokenInvalidated
import io.tolgee.exceptions.AuthExpiredException
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.service.security.UserAccountService
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.stereotype.Component

@Component
class OAuth2AccessTokenResolver(
  private val authorizationService: OAuth2AuthorizationService,
  private val userAccountService: UserAccountService,
) : Logging {
  fun tryResolve(token: String): TolgeeAuthentication? {
    // Tolgee's own JWTs are the other kind of Bearer token on this path. They always carry the two dots of a JWS,
    // which an opaque token (base64url, no padding) never does — so this rules them out without a store lookup.
    if (token.contains('.')) return null

    val authorization = findAuthorization(token) ?: return null
    val accessToken = authorization.accessToken ?: return null
    if (!accessToken.isActive) {
      throw AuthExpiredException(Message.OAUTH_TOKEN_EXPIRED)
    }

    val userId =
      authorization.principalName?.toLongOrNull() ?: throw AuthenticationException(Message.INVALID_OAUTH_TOKEN)
    val user =
      userAccountService.findDto(userId)
        ?: throw AuthenticationException(Message.INVALID_OAUTH_TOKEN)

    if (user.isTokenInvalidated(accessToken.token.issuedAt)) {
      throw AuthExpiredException(Message.OAUTH_TOKEN_EXPIRED)
    }

    return TolgeeAuthentication(
      credentials = OAuth2TokenCredentials(parseScopes(accessToken), parseProjects(accessToken)),
      deviceId = accessToken.claims?.get(JTI_CLAIM)?.toString(),
      userAccount = user,
      actingAsUserAccount = null,
      isReadOnly = false,
      isSuperToken = false,
    )
  }

  /**
   * Clients are registered from configuration, so one can be de-registered — which is how the docs say to switch a
   * client off — while its grants are still in the store. SAS's JDBC service throws when it cannot re-hydrate the
   * client for a row, which would answer every request carrying such a token with a 500 instead of a 401.
   */
  private fun findAuthorization(token: String): OAuth2Authorization? =
    try {
      authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN)
    } catch (e: DataRetrievalFailureException) {
      logger.debug("Ignoring an access token whose client is no longer registered", e)
      null
    }

  // A stored token can name a scope that no longer exists, so unknown values are dropped rather than raised.
  private fun parseScopes(accessToken: OAuth2Authorization.Token<OAuth2AccessToken>): Set<Scope> =
    accessToken.token.scopes
      .mapNotNull { SCOPES_BY_VALUE[it] }
      .toSet()

  private fun parseProjects(accessToken: OAuth2Authorization.Token<OAuth2AccessToken>): Set<Long>? {
    val raw = accessToken.claims?.get(OAuth2Constants.PROJECTS_CLAIM) ?: return null
    if (raw == OAuth2Constants.ALL_PROJECTS) return null
    if (raw is Collection<*>) {
      return raw.mapNotNull { it.toString().toLongOrNull() }.toSet()
    }
    // An unrecognized claim shape must not read as "all projects"; deny every project instead.
    return emptySet()
  }

  companion object {
    private const val JTI_CLAIM = "jti"
    private val SCOPES_BY_VALUE = Scope.entries.associateBy { it.value }
  }
}
