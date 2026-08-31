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
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.OAUTH_ACCESS_TOKEN_PREFIX
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.service.security.UserAccountService
import org.springframework.stereotype.Component

@Component
class OAuth2AccessTokenResolver(
  private val repository: OAuth2GrantRepository,
  private val clientRegistry: OAuth2ClientRegistry,
  private val userAccountService: UserAccountService,
  private val keyGenerator: KeyGenerator,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun tryResolve(token: String): TolgeeAuthentication? {
    // Tolgee's own JWTs are the other kind of Bearer token on this path; the prefix is what tells the two apart
    // without a store lookup.
    if (!token.startsWith(OAUTH_ACCESS_TOKEN_PREFIX)) return null

    val hash = keyGenerator.hash(token.removePrefix(OAUTH_ACCESS_TOKEN_PREFIX))
    val grant =
      repository.findByAccessTokenHash(hash) ?: throw AuthenticationException(Message.INVALID_OAUTH_TOKEN)
    val expiresAt = grant.accessTokenExpiresAt ?: throw AuthenticationException(Message.INVALID_OAUTH_TOKEN)
    if (!expiresAt.after(currentDateProvider.date)) {
      throw AuthExpiredException(Message.OAUTH_TOKEN_EXPIRED)
    }

    // A grant outlives the client it was issued to, so this is checked per request rather than at issue time.
    if (!clientRegistry.isStillAuthorized(grant.clientId)) {
      throw AuthenticationException(Message.INVALID_OAUTH_TOKEN)
    }

    val user =
      userAccountService.findDto(grant.userAccount.id)
        ?: throw AuthenticationException(Message.INVALID_OAUTH_TOKEN)
    if (user.isTokenInvalidated(grant.accessTokenIssuedAt?.toInstant())) {
      throw AuthExpiredException(Message.OAUTH_TOKEN_EXPIRED)
    }

    return TolgeeAuthentication(
      credentials = OAuth2TokenCredentials(grant.activeScopeSet(), grant.boundProjectIds()),
      deviceId = null,
      userAccount = user,
      actingAsUserAccount = null,
      isReadOnly = false,
      isSuperToken = false,
    )
  }
}
