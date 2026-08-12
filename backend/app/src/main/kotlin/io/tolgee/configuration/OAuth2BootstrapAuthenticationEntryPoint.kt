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

package io.tolgee.configuration

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Unauthenticated `/oauth2/authorize` → the SPA bootstrap page (authorize URL in `continue`), which turns the stored JWT into a session and returns. */
class OAuth2BootstrapAuthenticationEntryPoint(
  private val bootstrapPath: String,
) : AuthenticationEntryPoint {
  override fun commence(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authException: AuthenticationException,
  ) {
    val query = request.queryString?.let { "?$it" } ?: ""
    val continueUrl = request.requestURL.toString() + query
    val encoded = URLEncoder.encode(continueUrl, StandardCharsets.UTF_8)
    response.sendRedirect("$bootstrapPath?continue=$encoded")
  }
}
