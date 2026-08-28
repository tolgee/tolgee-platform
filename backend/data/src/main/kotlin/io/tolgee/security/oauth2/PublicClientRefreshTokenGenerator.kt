/**
 * Copyright 2020-2026 the original author or authors.
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Derived from `org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator` in
 * spring-security-oauth2-authorization-server (Apache-2.0). Changed: translated to Kotlin, and the
 * `isPublicClientForAuthorizationCodeGrant` skip removed so public clients receive refresh tokens.
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

import org.springframework.security.crypto.keygen.Base64StringKeyGenerator
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import java.time.Instant
import java.util.Base64

/**
 * Copy of Spring's `OAuth2RefreshTokenGenerator` minus its `isPublicClientForAuthorizationCodeGrant` skip, so public
 * clients get refresh tokens. Re-check this fork on SAS upgrades.
 */
class PublicClientRefreshTokenGenerator : OAuth2TokenGenerator<OAuth2RefreshToken> {
  private val refreshTokenGenerator: StringKeyGenerator =
    Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96)

  override fun generate(context: OAuth2TokenContext): OAuth2RefreshToken? {
    if (OAuth2TokenType.REFRESH_TOKEN != context.tokenType) {
      return null
    }
    val issuedAt = Instant.now()
    val expiresAt = issuedAt.plus(context.registeredClient.tokenSettings.refreshTokenTimeToLive)
    return OAuth2RefreshToken(refreshTokenGenerator.generateKey(), issuedAt, expiresAt)
  }
}
