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

package io.tolgee.security.authentication

import io.tolgee.API_KEY_HEADER_NAME
import jakarta.servlet.http.HttpServletRequest
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Where [AuthenticationFilter] looks for a credential. */
object CredentialPresence {
  const val AUTHORIZATION_HEADER = "Authorization"
  const val API_KEY_HEADER = API_KEY_HEADER_NAME
  const val API_KEY_QUERY_PARAM = "ak"

  /**
   * Reads the `ak` key out of the raw query string rather than via `getParameter`, which would parse — and so
   * consume — a form-encoded body before a caller that needs to read that body itself gets to see it.
   */
  fun hasAny(request: HttpServletRequest): Boolean {
    if (request.getHeader(AUTHORIZATION_HEADER) != null) return true
    if (request.getHeader(API_KEY_HEADER) != null) return true
    return hasQueryApiKey(request.queryString)
  }

  private fun hasQueryApiKey(queryString: String?): Boolean {
    val query = queryString ?: return false
    return query.split("&").any { decodedName(it.substringBefore('=')) == API_KEY_QUERY_PARAM }
  }

  /**
   * The servlet container percent-decodes parameter names before `getParameter` matches them, so `?%61k=` is a
   * credential to [AuthenticationFilter]. Reading the raw query string here without decoding would make the two
   * disagree about whether a request carried one.
   */
  private fun decodedName(raw: String): String =
    runCatching { URLDecoder.decode(raw, StandardCharsets.UTF_8) }.getOrDefault(raw)
}
