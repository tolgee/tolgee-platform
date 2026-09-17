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

import java.net.URI

/** The one place this subsystem decides what a URL's parts mean when two spellings have to compare equal. */
internal object UrlOrigins {
  /**
   * A URL that does not parse is an answer here, not an exception: these run on `/oauth2/authorize`, where the value
   * is chosen by an unauthenticated caller and anything thrown becomes a 500. Callers decide what a null means.
   */
  fun parse(value: String): URI? = runCatching { URI(value) }.getOrNull()

  /**
   * The RFC 6454 origin — the comparison key for "same-origin", with the port always written out so an implicit
   * port and its explicit form compare equal.
   */
  fun originOf(value: String): String? {
    val parsed = parse(value) ?: return null
    val scheme = parsed.lowercaseScheme ?: return null
    val host = parsed.host?.lowercase() ?: return null
    val port = parsed.port.takeIf { it != -1 } ?: defaultPort(scheme) ?: return null
    return "$scheme://$host:$port"
  }

  fun defaultPort(scheme: String): Int? =
    when (scheme) {
      "https" -> 443
      "http" -> 80
      else -> null
    }
}

/**
 * Schemes are case-insensitive, so a check reading [URI.getScheme] directly lets `HTTPS://` past a comparison
 * against `"https"`.
 */
internal val URI.lowercaseScheme: String? get() = scheme?.lowercase()
