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
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
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
) {
  /**
   * The issuer as advertised inside a request, falling back to the origin the container saw — not `X-Forwarded-*`,
   * which is untrusted, so behind a reverse proxy `tolgee.back-end-url` has to be set.
   */
  val issuerUrl: String
    get() =
      configuredBaseUrl ?: ServletUriComponentsBuilder
        .fromCurrentContextPath()
        .build()
        .toUriString()
        .trimEnd('/')

  val configuredBaseUrl: String?
    get() = tolgeeProperties.backEndUrl.normalized() ?: tolgeeProperties.frontEndUrl.normalized()

  /**
   * The issuer is concatenated with endpoint paths, so `https://host/` would publish `https://host//oauth2/token`
   * while another consumer advertises `https://host` — a client following one and validating against the other sees
   * two different servers.
   */
  private fun String?.normalized(): String? {
    val trimmed = this?.takeIf { it.isNotBlank() }?.trimEnd('/') ?: return null
    requireOrigin(trimmed)
    return trimmed
  }

  /**
   * RFC 8414 §3 puts the metadata document at `https://host/.well-known/oauth-authorization-server<issuer path>`,
   * which Tolgee does not serve; a path-prefixed issuer would also make every endpoint URL it publishes unreachable.
   * Failing here names the misconfigured property instead of leaving the flow to dead-end in the browser.
   */
  private fun requireOrigin(url: String) {
    // A value that does not parse must not slip through: getOrNull() would make `path` null, which reads as "no path"
    // and would pass the check below, leaving the unparseable URL to 500 later wherever it is concatenated.
    val parsed =
      runCatching { URI(url) }.getOrNull()
        ?: throw IllegalStateException(
          "tolgee.back-end-url (or tolgee.front-end-url) is not a valid URL: $url",
        )
    val path = parsed.path
    if (!path.isNullOrEmpty()) {
      throw IllegalStateException(
        "tolgee.back-end-url (or tolgee.front-end-url) must be a bare origin with no path for OAuth2 to work, got: $url",
      )
    }
  }
}
