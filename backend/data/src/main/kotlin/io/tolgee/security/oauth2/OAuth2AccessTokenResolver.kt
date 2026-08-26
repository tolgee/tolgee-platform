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
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.stereotype.Component

/**
 * Resolves an opaque OAuth2 access token by looking the grant up in `oauth2_authorization`.
 *
 * Because the lookup happens on every request, deleting the row revokes the token immediately — that is the point of
 * issuing opaque tokens rather than self-contained ones.
 */
@Component
class OAuth2AccessTokenResolver(
  private val authorizationService: OAuth2AuthorizationService,
  private val userAccountService: UserAccountService,
) {
  fun tryResolve(token: String): TolgeeAuthentication? {
    // Tolgee's own JWTs are the other kind of Bearer token on this path. They always carry the two dots of a JWS,
    // which an opaque token (base64url, no padding) never does — so this rules them out without a store lookup.
    if (token.contains('.')) return null

    val authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN) ?: return null
    val accessToken = authorization.accessToken ?: return null
    if (!accessToken.isActive) {
      throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
    }

    val userId = authorization.principalName?.toLongOrNull() ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    val user =
      userAccountService.findDto(userId)
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    if (user.isTokenInvalidated(accessToken.token.issuedAt)) {
      throw AuthExpiredException(Message.EXPIRED_JWT_TOKEN)
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

  private fun parseScopes(accessToken: OAuth2Authorization.Token<OAuth2AccessToken>): Set<Scope> =
    accessToken.token.scopes
      .mapNotNull { runCatching { Scope.fromValue(it) }.getOrNull() }
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
  }
}
