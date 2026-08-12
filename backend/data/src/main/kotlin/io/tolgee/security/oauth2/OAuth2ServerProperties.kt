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
  /** Browser-extension redirect URIs (the id depends on the packaged key, so it's configured). Empty = client not registered. */
  var browserExtensionRedirectUris: List<String> = listOf()

  /** Loopback redirect URIs for the Tolgee CLI (RFC 8252). */
  var cliRedirectUris: List<String> = listOf("http://127.0.0.1:9876/callback")

  var accessTokenValidityMinutes: Long = 30

  var refreshTokenValidityDays: Long = 30

  var authorizationRetentionDays: Long = 7

  fun tokenSettings(): TokenSettings {
    return TokenSettings
      .builder()
      .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
      .accessTokenTimeToLive(Duration.ofMinutes(accessTokenValidityMinutes))
      .refreshTokenTimeToLive(Duration.ofDays(refreshTokenValidityDays))
      .reuseRefreshTokens(false)
      .build()
  }
}
