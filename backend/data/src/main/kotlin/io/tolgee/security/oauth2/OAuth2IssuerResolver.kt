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

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.util.nullIfBlank
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.net.URI

/**
 * The URL that identifies this authorization server.
 *
 * Deliberately not [io.tolgee.component.FrontendUrlProvider]: every OAuth endpoint hangs off the issuer, so this must
 * be where the *API* is reachable. `front-end-url` is only a fallback for single-origin deployments, where the backend
 * also serves the SPA and operators commonly leave `back-end-url` unset.
 */
@Component
class OAuth2IssuerResolver(
  private val tolgeeProperties: TolgeeProperties,
  private val clientRegistry: OAuth2ClientRegistry,
) {
  @PostConstruct
  fun requireConfiguredIssuer() {
    if (clientRegistry.isEnabled) issuerUrl
  }

  val issuerUrl: String
    get() =
      checkNotNull(configuredBaseUrl) {
        "tolgee.back-end-url (or tolgee.front-end-url) must be set when a tolgee.oauth2 client is configured: the " +
          "issuer is published in every discovery document and on every authorization response, and it is never " +
          "derived from the request"
      }

  private val configuredBaseUrl: String?
    get() = tolgeeProperties.backEndUrl.normalized() ?: tolgeeProperties.frontEndUrl.normalized()

  /**
   * The issuer is concatenated with endpoint paths, so `https://host/` would publish `https://host//oauth2/token`
   * while another consumer advertises `https://host` — a client following one and validating against the other sees
   * two different servers.
   */
  private fun String?.normalized(): String? {
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
