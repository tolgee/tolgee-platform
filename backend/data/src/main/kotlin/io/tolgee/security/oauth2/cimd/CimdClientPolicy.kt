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

import io.tolgee.configuration.tolgee.InternalProperties
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import org.springframework.stereotype.Component
import java.net.URI

/**
 * Decides whether an unknown (non-pre-registered) `client_id` should be resolved through the CIMD path at all.
 *
 * This is a cheap, no-network routing decision, not the security validation: it requires a well-formed https URL (http
 * only where SSRF protection is disabled for dev, mirroring [CimdDocumentFetcher]) on a
 * [OAuth2ServerProperties.cimdAllowedHosts]-permitted host. The SSRF-hardening (no loopback/private targets, DNS
 * pinning) is enforced later, when the document is fetched. Kept separate from the fetch so `isStillAuthorized` can
 * reuse it without touching the network — and it must agree with the fetcher on which schemes can ever resolve, so
 * `isStillAuthorized` never claims a client the fetcher would refuse.
 */
@Component
class CimdClientPolicy(
  private val properties: OAuth2ServerProperties,
  private val internalProperties: InternalProperties,
) {
  fun isCandidate(clientId: String): Boolean {
    val uri = runCatching { URI(clientId) }.getOrNull() ?: return false
    if (!isAllowedScheme(uri.scheme?.lowercase())) return false
    val host = uri.host ?: return false
    return isHostAllowed(host)
  }

  private fun isAllowedScheme(scheme: String?): Boolean {
    if (scheme == "https") return true
    return scheme == "http" && internalProperties.disableUrlSsrfProtection
  }

  private fun isHostAllowed(host: String): Boolean {
    val allowed = properties.cimdAllowedHosts
    if (allowed.isEmpty()) return true
    return host in allowed
  }
}
