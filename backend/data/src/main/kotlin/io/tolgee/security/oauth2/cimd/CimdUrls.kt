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

import io.tolgee.security.oauth2.UrlOrigins
import io.tolgee.security.oauth2.lowercaseScheme

/** The shape a `client_id` must have before anything will route it, or fetch it, through the CIMD path. */
internal object CimdUrls {
  /** The grant's `client_id` column is VARCHAR(255); a longer-but-valid URL would pass only to fail at insert. */
  const val MAX_CLIENT_ID_LENGTH = 255

  fun isAcceptableClientId(
    clientId: String,
    allowHttp: Boolean,
  ): Boolean {
    if (clientId.length > MAX_CLIENT_ID_LENGTH) return false
    // The CIMD draft requires an https URL with no fragment. A fragment never reaches the request target anyway, so
    // accepting one would let the same document answer under many client_ids that a user cannot tell apart.
    if (clientId.contains('#')) return false
    // A query string is never needed to name a document, and it is the cheapest way to mint unlimited distinct
    // client_ids at one host: each one is a fresh cache entry and a fresh outbound GET at that host.
    if (clientId.contains('?')) return false
    // Spelled in lower case, not merely parsing to it. The `client_id` is stored and matched as the string the
    // caller sent, and every durable control keys off `LIKE 'http%'`, which Postgres compares case-sensitively -
    // so `HTTPS://x/y` would resolve, consent and hold a grant while being invisible to the withdrawal check, the
    // staleness bound and the per-account cap.
    if (!clientId.startsWith("https://") && !(allowHttp && clientId.startsWith("http://"))) return false
    val scheme = UrlOrigins.parse(clientId)?.lowercaseScheme ?: return false
    return scheme == "https" || (allowHttp && scheme == "http")
  }
}
