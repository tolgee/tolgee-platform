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

package io.tolgee.security.oauth2.cimd

import io.tolgee.component.FrontendUrlProvider
import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.security.oauth2.OAuth2IssuerResolver
import io.tolgee.security.oauth2.UrlOrigins
import org.springframework.stereotype.Component

/** Routing only — the SSRF-hardening is in [CimdDocumentFetcher]. */
@Component
class CimdClientPolicy(
  private val properties: OAuth2ServerProperties,
  private val internalProperties: InternalProperties,
  private val issuerResolver: OAuth2IssuerResolver,
  private val frontendUrlProvider: FrontendUrlProvider,
) {
  fun isCandidate(clientId: String): Boolean {
    if (!properties.cimdEnabled) return false
    if (!CimdUrls.isAcceptableClientId(clientId, allowHttp = internalProperties.disableUrlSsrfProtection)) return false
    if (isOwnOrigin(clientId)) return false
    val host = UrlOrigins.parse(clientId)?.host ?: return false
    return isHostAllowed(host)
  }

  /**
   * The consent screen shows the client's origin as its identity, and for an unverified client that line is the
   * whole anti-phishing signal. An id served from this instance's own origin would print the one host the user has
   * no reason to distrust — and its same-origin redirect URIs would be URLs on Tolgee itself.
   *
   * Both of this instance's origins, not only the issuer. Where `back-end-url` and `front-end-url` differ the
   * issuer is the back end, while the consent screen the user is looking at is served from the front end - which
   * is the origin the anti-phishing line would be printing.
   */
  private fun isOwnOrigin(clientId: String): Boolean {
    val origin = UrlOrigins.originOf(clientId) ?: return false
    return origin in ownOrigins()
  }

  private fun ownOrigins(): Set<String> =
    setOfNotNull(
      runCatching { issuerResolver.issuerUrl }.getOrNull()?.let { UrlOrigins.originOf(it) },
      frontendUrlProvider.stableUrl?.let { UrlOrigins.originOf(it) },
    )

  private fun isHostAllowed(host: String): Boolean {
    val allowed = properties.cimdAllowedHosts
    if (allowed.isEmpty()) return true
    return allowed.any { it.equals(host, ignoreCase = true) }
  }
}
