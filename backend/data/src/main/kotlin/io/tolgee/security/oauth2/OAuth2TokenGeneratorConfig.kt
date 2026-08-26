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

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator

/**
 * Tokens are opaque ([org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat.REFERENCE]),
 * so there is no signing key, no JWKS and no JWT encoder anywhere in the authorization server. Spring only registers
 * the JWK-set endpoint when a `JWKSource` bean exists, and only builds a `JwtGenerator` when a `JwtEncoder` exists —
 * publishing either would put a key lifecycle back into the server for no consumer.
 */
@Configuration
class OAuth2TokenGeneratorConfig {
  @Bean
  fun oauth2TokenGenerator(
    accessTokenCustomizer: OAuth2TokenCustomizer<OAuth2TokenClaimsContext>,
  ): OAuth2TokenGenerator<OAuth2Token> {
    val accessTokenGenerator = OAuth2AccessTokenGenerator()
    accessTokenGenerator.setAccessTokenCustomizer(accessTokenCustomizer)
    return DelegatingOAuth2TokenGenerator(
      accessTokenGenerator,
      PublicClientRefreshTokenGenerator(),
    )
  }
}
