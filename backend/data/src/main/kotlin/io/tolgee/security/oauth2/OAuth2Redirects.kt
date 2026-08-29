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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** The redirect URLs the authorization endpoint answers a client with. */
object OAuth2Redirects {
  /** RFC 6749 §4.1.2.1 error response. Carries the RFC 9207 `iss` too — §2 covers error responses. */
  fun error(
    redirectUri: String,
    error: OAuth2Error,
    issuer: String,
    state: String?,
  ): String {
    return appendQuery(
      redirectUri,
      listOf(
        "error" to error.error,
        "iss" to issuer,
        "error_description" to error.description,
        "state" to state,
      ),
    )
  }

  /** RFC 6749 §4.1.2 success response, plus the RFC 9207 `iss` so a client can tell which server answered. */
  fun code(
    redirectUri: String,
    code: String,
    issuer: String,
    state: String?,
  ): String {
    return appendQuery(redirectUri, listOf("code" to code, "iss" to issuer, "state" to state))
  }

  /**
   * RFC 3986 percent-encoding: a space becomes `%20`, never a bare `+`.
   *
   * `+` reads as a space to a form decoder and as a literal plus to a strict percent decoder, and RFC 6749 does not
   * say which a client uses. A `state` that comes back byte-different is one the client must treat as an attack
   * (§10.12), so the encoding has to be unambiguous to both.
   */
  fun encodeQueryValue(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

  /** Appends [params] to [url], skipping the null ones, encoding each value with [encodeQueryValue]. */
  fun appendQuery(
    url: String,
    params: List<Pair<String, String?>>,
  ): String {
    val query = params.mapNotNull { (name, value) -> value?.let { "$name=${encodeQueryValue(it)}" } }
    if (query.isEmpty()) return url
    val separator = if ("?" in url) "&" else "?"
    return url + separator + query.joinToString("&")
  }
}
