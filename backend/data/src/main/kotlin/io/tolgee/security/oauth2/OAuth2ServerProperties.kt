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

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration

@ConfigurationProperties(prefix = "tolgee.oauth2")
class OAuth2ServerProperties {
  var browserExtensionRedirectUris: List<String> = listOf()

  /**
   * Loopback redirect URIs for the Tolgee CLI (RFC 8252).
   *
   * Empty by default, like [browserExtensionRedirectUris]: a client that exists on every instance is a client any
   * local process can start a consent prompt against, and nothing ships that uses this one yet.
   */
  var cliRedirectUris: List<String> = listOf()

  var accessTokenValidityMinutes: Long = 30

  var refreshTokenValidityDays: Long = 30

  var authorizationRetentionDays: Long = 7

  fun tokenSettings(): TokenSettings {
    return TokenSettings
      .builder()
      // Opaque, not self-contained: the token is looked up in oauth2_authorization on every request, so revoking a
      // grant kills its access tokens at once. A signed JWT would need its own signing-key lifecycle and could only be
      // revoked on expiry. Tolgee's other credentials (PAK, PAT) are opaque and looked up the same way.
      .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
      .accessTokenTimeToLive(Duration.ofMinutes(accessTokenValidityMinutes))
      .refreshTokenTimeToLive(Duration.ofDays(refreshTokenValidityDays))
      .reuseRefreshTokens(false)
      .build()
  }
}
