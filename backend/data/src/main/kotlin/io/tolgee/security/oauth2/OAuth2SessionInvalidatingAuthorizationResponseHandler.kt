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

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken
import org.springframework.security.web.DefaultRedirectStrategy
import org.springframework.security.web.RedirectStrategy
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets

/**
 * Ends the HTTP session when the authorization code is issued, so the next connect re-runs the session bootstrap and
 * mints a token for whoever is signed into the webapp now, not a stale principal left in the session cookie.
 *
 * The redirect is reproduced from `OAuth2AuthorizationEndpointFilter.sendAuthorizationResponse` because the default is
 * a private method reference that can't be wrapped (RFC 6749 §4.1.2 success response: code + optional state).
 */
class OAuth2SessionInvalidatingAuthorizationResponseHandler(
  private val issuerResolver: OAuth2IssuerResolver,
  private val redirectStrategy: RedirectStrategy = DefaultRedirectStrategy(),
) : AuthenticationSuccessHandler {
  override fun onAuthenticationSuccess(
    request: HttpServletRequest,
    response: HttpServletResponse,
    authentication: Authentication,
  ) {
    val token = authentication as OAuth2AuthorizationCodeRequestAuthenticationToken
    val code = token.authorizationCode ?: throw IllegalStateException("authorizationCode cannot be null")
    val redirectUri = token.redirectUri ?: throw IllegalStateException("redirectUri cannot be null")

    val uriBuilder =
      UriComponentsBuilder
        .fromUriString(redirectUri)
        .queryParam(OAuth2ParameterNames.CODE, code.tokenValue)
    // RFC 9207 iss (AS mix-up defense). SAS 7.1 does not emit it — its response carries only code and state — so a
    // client that requires iss breaks if this handler stops adding it.
    uriBuilder.queryParam("iss", UriUtils.encode(issuerResolver.issuerUrl, StandardCharsets.UTF_8))
    val state = token.state
    if (!state.isNullOrBlank()) {
      uriBuilder.queryParam(OAuth2ParameterNames.STATE, UriUtils.encode(state, StandardCharsets.UTF_8))
    }

    // Invalidate before the redirect flushes the response; after that the session cookie can no longer be expired.
    request.getSession(false)?.invalidate()
    redirectStrategy.sendRedirect(request, response, uriBuilder.build(true).toUriString())
  }
}
