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

package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration

@ConfigurationProperties(prefix = "tolgee.oauth2")
@DocProperty(
  description = "Settings for Tolgee acting as an OAuth 2.1 authorization server (browser-extension login, MCP).",
  displayName = "OAuth2 authorization server",
)
class OAuth2ServerProperties {
  @DocProperty(
    description =
      "Exact redirect URIs of the Tolgee browser extension, e.g. `https://<extension-id>.chromiumapp.org/`. " +
        "The extension OAuth client is only registered when this is set.",
    defaultValue = "",
  )
  var browserExtensionRedirectUris: List<String> = listOf()

  @DocProperty(
    description =
      "Loopback redirect URIs of the Tolgee CLI (RFC 8252), e.g. `http://127.0.0.1:9876/callback`. The CLI " +
        "OAuth client is only registered when this is set.\n" +
        "\n" +
        ":::info\n" +
        "A loopback redirect cannot be tied to one local application, so any process on the machine that knows " +
        "the client id can start an authorization for it. The user still has to approve the consent screen, " +
        "but leave this unset unless the CLI is actually in use.\n" +
        ":::\n\n",
    defaultValue = "",
  )
  var cliRedirectUris: List<String> = listOf()

  @DocProperty(description = "How long an issued OAuth access token stays valid, in minutes.")
  var accessTokenValidityMinutes: Long = 30

  @DocProperty(description = "How long an issued OAuth refresh token stays valid, in days.")
  var refreshTokenValidityDays: Long = 30

  @DocProperty(
    description =
      "How long a spent or abandoned OAuth authorization is kept before it is deleted, in days. " +
        "Only rows whose tokens have all expired are removed.",
  )
  var authorizationRetentionDays: Long = 7

  fun tokenSettings(): TokenSettings {
    return TokenSettings
      .builder()
      // Opaque: looked up in oauth2_authorization per request, so deleting a grant revokes its access tokens at once.
      // See docs/oauth/README.md for why this beats a signed JWT here.
      .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
      .accessTokenTimeToLive(Duration.ofMinutes(accessTokenValidityMinutes))
      .refreshTokenTimeToLive(Duration.ofDays(refreshTokenValidityDays))
      .reuseRefreshTokens(false)
      .build()
  }
}
