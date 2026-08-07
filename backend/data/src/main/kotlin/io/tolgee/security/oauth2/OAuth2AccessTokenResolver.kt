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

import com.nimbusds.jwt.SignedJWT
import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.service.security.UserAccountService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

/**
 * Resolves an authorization-server access token into a [TolgeeAuthentication].
 *
 * Dispatch is by the `aud` claim (the AS stamps `apiAudience`, the legacy webapp JWT carries `tg.tok`), so token-family
 * identity is independent of the signing algorithm; a non-matching `aud` returns null and falls to
 * [io.tolgee.security.authentication.JwtService].
 */
@Component
class OAuth2AccessTokenResolver(
  @Qualifier("oauth2AccessTokenDecoder")
  private val decoder: JwtDecoder,
  private val userAccountService: UserAccountService,
  private val audienceResolver: OAuth2AudienceResolver,
) {
  fun tryResolve(token: String): TolgeeAuthentication? {
    if (!hasOAuthAudience(token)) return null

    val jwt = decode(token)
    validateAudience(jwt)

    val userId = jwt.subject?.toLongOrNull() ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    val user =
      userAccountService.findDto(userId)
        ?: throw AuthenticationException(Message.INVALID_JWT_TOKEN)

    return TolgeeAuthentication(
      credentials = OAuth2TokenCredentials(parseScopes(jwt), parseProjects(jwt)),
      deviceId = jwt.id,
      userAccount = user,
      actingAsUserAccount = null,
      isReadOnly = false,
      isSuperToken = false,
    )
  }

  private fun hasOAuthAudience(token: String): Boolean {
    return try {
      SignedJWT
        .parse(token)
        .jwtClaimsSet.audience
        ?.contains(audienceResolver.apiAudience) == true
    } catch (_: Exception) {
      false
    }
  }

  private fun decode(token: String): Jwt {
    return try {
      decoder.decode(token)
    } catch (_: JwtException) {
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }
  }

  private fun validateAudience(jwt: Jwt) {
    if (jwt.audience?.contains(audienceResolver.apiAudience) != true) {
      throw AuthenticationException(Message.INVALID_JWT_TOKEN)
    }
  }

  private fun parseScopes(jwt: Jwt): Set<Scope> {
    val raw = jwt.claims["scope"]
    val values =
      when (raw) {
        is Collection<*> -> raw.map { it.toString() }
        is String -> raw.split(" ")
        else -> emptyList()
      }
    return values
      .filter { it.isNotBlank() }
      .mapNotNull { runCatching { Scope.fromValue(it) }.getOrNull() }
      .toSet()
  }

  private fun parseProjects(jwt: Jwt): Set<Long>? {
    val raw = jwt.claims[OAuth2Constants.PROJECTS_CLAIM] ?: return null
    if (raw == OAuth2Constants.ALL_PROJECTS) return null
    if (raw is Collection<*>) {
      return raw.mapNotNull { it.toString().toLongOrNull() }.toSet()
    }
    return emptySet()
  }
}
