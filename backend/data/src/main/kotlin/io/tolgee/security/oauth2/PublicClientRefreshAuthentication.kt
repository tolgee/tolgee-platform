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
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.web.authentication.AuthenticationConverter

/**
 * Authenticates a bare `client_id` on the refresh grant for public (`NONE`) clients, which SAS otherwise can't. Gated
 * to `grant_type=refresh_token` to keep the code exchange PKCE-only.
 */
class PublicClientRefreshAuthenticationConverter : AuthenticationConverter {
  override fun convert(request: HttpServletRequest): Authentication? {
    if (request.getParameter(OAuth2ParameterNames.GRANT_TYPE) != AuthorizationGrantType.REFRESH_TOKEN.value) {
      return null
    }
    if (request.getHeader(HttpHeaders.AUTHORIZATION) != null ||
      request.getParameter(OAuth2ParameterNames.CLIENT_SECRET) != null
    ) {
      return null
    }
    val clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID) ?: return null
    return OAuth2ClientAuthenticationToken(
      clientId,
      ClientAuthenticationMethod.NONE,
      null,
      mapOf(OAuth2ParameterNames.GRANT_TYPE to AuthorizationGrantType.REFRESH_TOKEN.value),
    )
  }
}

class PublicClientRefreshAuthenticationProvider(
  private val registeredClientRepository: RegisteredClientRepository,
) : AuthenticationProvider {
  override fun authenticate(authentication: Authentication): Authentication? {
    val token = authentication as OAuth2ClientAuthenticationToken
    if (token.additionalParameters[OAuth2ParameterNames.GRANT_TYPE] != AuthorizationGrantType.REFRESH_TOKEN.value) {
      return null
    }
    val clientId = token.principal as? String ?: return null
    val registeredClient =
      registeredClientRepository.findByClientId(clientId)
        ?: throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT)
    if (!registeredClient.clientAuthenticationMethods.contains(ClientAuthenticationMethod.NONE) ||
      !registeredClient.authorizationGrantTypes.contains(AuthorizationGrantType.REFRESH_TOKEN)
    ) {
      throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT)
    }
    return OAuth2ClientAuthenticationToken(registeredClient, ClientAuthenticationMethod.NONE, null)
  }

  override fun supports(authentication: Class<*>): Boolean =
    OAuth2ClientAuthenticationToken::class.java.isAssignableFrom(authentication)
}
