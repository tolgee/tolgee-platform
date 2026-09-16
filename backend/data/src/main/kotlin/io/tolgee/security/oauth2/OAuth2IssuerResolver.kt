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

import io.tolgee.component.BackendUrlProvider
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.util.nullIfBlank
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.net.URI

/**
 * The URL that identifies this authorization server.
 *
 * Built from the providers' *stable* values only, never
 * [io.tolgee.component.FrontendUrlProvider.requestDerivedUrl]: the issuer is published in discovery and as `iss` on
 * every authorization response, so a request-derived value would let a caller choose what this server says about
 * itself.
 *
 * Fallback order and its rationale: docs/oauth/README.md, ### issuer.
 */
@Component
class OAuth2IssuerResolver(
  private val backendUrlProvider: BackendUrlProvider,
  private val frontendUrlProvider: FrontendUrlProvider,
) {
  /**
   * Fail loudly when the issuer is *set but malformed* (a path, query or fragment): the authorization server would
   * publish an unreachable endpoint. A simply-unset issuer is not an error — it just means the server is off.
   */
  @PostConstruct
  fun validateConfiguredIssuer() {
    configuredBaseUrl
  }

  /**
   * Whether this instance is configured to act as an OAuth authorization server. Enabling is issuer-based, not
   * client-based: the endpoints and CIMD go live on any instance with a usable issuer, so a fresh self-hosted
   * instance can accept unknown MCP clients before any client is pre-registered.
   */
  val isConfigured: Boolean
    get() = runCatching { configuredBaseUrl }.getOrNull() != null

  val issuerUrl: String
    get() =
      checkNotNull(configuredBaseUrl) {
        "tolgee.back-end-url (or tolgee.front-end-url) must be set for Tolgee to act as an OAuth 2.1 authorization " +
          "server: the issuer is published in every discovery document and on every authorization response, and it " +
          "is never derived from the request"
      }

  private val configuredBaseUrl: String?
    get() = backendUrlProvider.stableUrl.requireValidOrigin() ?: frontendUrlProvider.stableUrl.requireValidOrigin()

  /**
   * The issuer is concatenated with endpoint paths, so `https://host/` would publish `https://host//oauth2/token`
   * while another consumer advertises `https://host` — a client following one and validating against the other sees
   * two different servers.
   */
  private fun String?.requireValidOrigin(): String? {
    val trimmed = this.nullIfBlank?.trimEnd('/') ?: return null
    requireOrigin(trimmed)
    return trimmed
  }

  /**
   * RFC 8414 §2 defines the issuer as a URL with no query or fragment, and §3 puts the metadata document at
   * `https://host/.well-known/oauth-authorization-server<issuer path>`, which Tolgee does not serve; any of the three
   * would also make every endpoint URL the issuer is concatenated into unreachable.
   */
  private fun requireOrigin(url: String) {
    val parsed =
      runCatching { URI(url) }.getOrNull()
        ?: throw IllegalStateException(
          "tolgee.back-end-url (or tolgee.front-end-url) is not a valid URL: $url",
        )
    if (!parsed.path.isNullOrEmpty() || parsed.query != null || parsed.fragment != null) {
      throw IllegalStateException(
        "tolgee.back-end-url (or tolgee.front-end-url) must be a bare origin with no path, query or fragment " +
          "for OAuth2 to work, got: $url",
      )
    }
  }
}
