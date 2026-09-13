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

import io.tolgee.mcp.McpConstants
import io.tolgee.security.OAUTH_ACCESS_TOKEN_PREFIX
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.util.UrlPathHelper

/**
 * The RFC 6750 §3 `WWW-Authenticate` challenge a protected resource owes a caller it refused.
 */
@Component
class OAuth2BearerChallengeProvider(
  private val issuerResolver: OAuth2IssuerResolver,
  private val clientRegistry: OAuth2ClientRegistry,
) {
  fun challengeFor(
    request: HttpServletRequest,
    status: HttpStatus,
  ): String? {
    if (hasOAuthCredentials(request)) {
      if (status == HttpStatus.UNAUTHORIZED) return challenge("invalid_token", request)
      if (status == HttpStatus.FORBIDDEN) return challenge("insufficient_scope", request)
      return null
    }
    // RFC 6750 §3.1: "If the request lacks any authentication information ... the resource server SHOULD NOT include
    // an error code or other error information." Only the MCP resource publishes metadata a discovering client can
    // act on, so it is the only path where a caller with no OAuth token is worth challenging at all.
    if (status == HttpStatus.UNAUTHORIZED && isProtectedResourcePath(request)) return challenge(null, request)
    return null
  }

  private fun challenge(
    error: String?,
    request: HttpServletRequest,
  ): String {
    val parameters = mutableListOf<String>()
    error?.let { parameters += """error="$it"""" }
    resourceMetadataUrl(request)?.let { parameters += """resource_metadata="$it"""" }
    if (parameters.isEmpty()) return "Bearer"
    return "Bearer " + parameters.joinToString(", ")
  }

  /**
   * Tolgee's own webapp authenticates with `Authorization: Bearer <JWT>`, so the scheme alone would make every
   * ordinary refusal in the product look like an OAuth one. Only a token carrying the OAuth prefix is an OAuth
   * caller.
   */
  private fun hasOAuthCredentials(request: HttpServletRequest): Boolean {
    val header = request.getHeader("Authorization") ?: return false
    if (!header.startsWith("Bearer ")) return false
    return header.removePrefix("Bearer ").startsWith(OAUTH_ACCESS_TOKEN_PREFIX)
  }

  private fun isProtectedResourcePath(request: HttpServletRequest): Boolean =
    UrlPathHelper.defaultInstance
      .getPathWithinApplication(request)
      .startsWith(McpConstants.DEVELOPER_ENDPOINT_PATH)

  // RFC 9728 §5.1: the challenge names the metadata document for the resource that was requested, and Tolgee publishes
  // one only for the MCP resource.
  private fun resourceMetadataUrl(request: HttpServletRequest): String? {
    if (!isProtectedResourcePath(request)) return null
    if (!clientRegistry.isEnabled) return null
    // This runs from inside the exception handler. A misconfigured issuer throws, and letting that escape would
    // replace every handled error on the MCP path with a 500 raised while rendering it.
    val issuer = runCatching { issuerResolver.issuerUrl }.getOrNull() ?: return null
    return issuer + OAuth2Constants.PROTECTED_RESOURCE_METADATA_PATH
  }
}
