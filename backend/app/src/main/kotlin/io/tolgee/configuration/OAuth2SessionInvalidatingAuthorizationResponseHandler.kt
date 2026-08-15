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
 * Replaces SAS's default authorization-response handler so the HTTP session dies the moment an authorization code is
 * issued. The session exists only to carry the single authorize -> consent -> authorize round trip (the token and
 * refresh exchanges are back-channel and sessionless). Ending it here forces the next connect to re-run the session
 * bootstrap, so a token is always minted for whoever is signed into the webapp now — not a stale principal still sitting
 * in the session cookie after the webapp user switched accounts. Revoking the consent on disconnect would not fix this:
 * it only re-shows the consent screen while the reconnect still authenticates as the stale principal (and would break
 * same-account consent-skip). Consent revocation stays reserved for an explicit app-revocation, not routine disconnect.
 *
 * The redirect is reproduced from `OAuth2AuthorizationEndpointFilter.sendAuthorizationResponse` (the default is a
 * private method reference that can't be wrapped); it is the RFC 6749 §4.1.2 success response (code + optional state).
 */
class OAuth2SessionInvalidatingAuthorizationResponseHandler(
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
    val state = token.state
    if (!state.isNullOrBlank()) {
      uriBuilder.queryParam(OAuth2ParameterNames.STATE, UriUtils.encode(state, StandardCharsets.UTF_8))
    }

    // Invalidate before the redirect commits the response: once it flushes, Spring Session can no longer expire the
    // session cookie.
    request.getSession(false)?.invalidate()
    redirectStrategy.sendRedirect(request, response, uriBuilder.build(true).toUriString())
  }
}
