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

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.isTokenInvalidated
import io.tolgee.exceptions.AuthExpiredException
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.enums.Scope
import io.tolgee.model.oauth2.OAuth2Authorization
import io.tolgee.repository.oauth2.OAuth2AuthorizationRepository
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Component

@Component
class OAuth2AccessTokenResolver(
  private val repository: OAuth2AuthorizationRepository,
  private val clientRegistry: OAuth2ClientRegistry,
  private val userAccountService: UserAccountService,
  private val keyGenerator: KeyGenerator,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun tryResolve(token: String): TolgeeAuthentication? {
    // Tolgee's own JWTs are the other kind of Bearer token on this path. They always carry the two dots of a JWS,
    // which an opaque token never does — so this rules them out without a store lookup.
    if (token.contains('.')) return null

    val authorization = repository.findByAccessTokenHash(keyGenerator.hash(token)) ?: return null
    val expiresAt = authorization.accessTokenExpiresAt ?: throw AuthenticationException(Message.INVALID_OAUTH_TOKEN)
    if (!expiresAt.after(currentDateProvider.date)) {
      throw AuthExpiredException(Message.OAUTH_TOKEN_EXPIRED)
    }

    // Clients are registered from configuration, and dropping one is how an operator switches it off; its grants
    // outlive it in the store and must stop authenticating.
    clientRegistry.find(authorization.clientId) ?: throw AuthenticationException(Message.INVALID_OAUTH_TOKEN)

    val user =
      userAccountService.findDto(authorization.userAccount.id)
        ?: throw AuthenticationException(Message.INVALID_OAUTH_TOKEN)
    if (user.isTokenInvalidated(authorization.accessTokenIssuedAt?.toInstant())) {
      throw AuthExpiredException(Message.OAUTH_TOKEN_EXPIRED)
    }

    return TolgeeAuthentication(
      credentials = OAuth2TokenCredentials(grantedScopes(authorization), authorization.boundProjectIds()),
      deviceId = authorization.id.toString(),
      userAccount = user,
      actingAsUserAccount = null,
      isReadOnly = false,
      isSuperToken = false,
    )
  }

  // A stored scope value can name a scope that no longer exists, so unknown values are dropped rather than raised.
  private fun grantedScopes(authorization: OAuth2Authorization): Set<Scope> =
    authorization.grantedScopeValues.mapNotNull { SCOPES_BY_VALUE[it] }.toSet()

  companion object {
    private val SCOPES_BY_VALUE = Scope.entries.associateBy { it.value }
  }
}
