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
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

/**
 * The RFC 6750 §3 `WWW-Authenticate` challenge a protected resource owes a caller it refused.
 *
 * It is also how an RFC 9728 client discovers this deployment's authorization server: the `resource_metadata`
 * parameter points at the protected-resource document, which is the only machine-readable route from a 401 to
 * `/oauth2/authorize`.
 */
@Component
class OAuth2BearerChallengeProvider(
  private val issuerResolver: OAuth2IssuerResolver,
) {
  fun challengeFor(
    request: HttpServletRequest,
    status: HttpStatus,
  ): String? {
    // RFC 6750 §3.1: "If the request lacks any authentication information ... the resource server SHOULD NOT include
    // an error code or other error information." A caller that presented nothing is being told how to authenticate,
    // not that its token is bad.
    if (status == HttpStatus.UNAUTHORIZED) {
      return challenge(if (hasBearerCredentials(request)) "invalid_token" else null, request)
    }
    if (status == HttpStatus.FORBIDDEN && hasBearerCredentials(request)) {
      return challenge("insufficient_scope", request)
    }
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

  private fun hasBearerCredentials(request: HttpServletRequest): Boolean =
    request.getHeader("Authorization")?.startsWith("Bearer ") == true

  // RFC 9728 §5.1: the challenge names the metadata document for the resource that was requested, and Tolgee publishes
  // one only for the MCP resource.
  private fun resourceMetadataUrl(request: HttpServletRequest): String? {
    val path = request.requestURI.removePrefix(request.contextPath)
    if (!path.startsWith(OAuth2Constants.MCP_RESOURCE_PATH)) return null
    return issuerResolver.issuerUrl + OAuth2Constants.PROTECTED_RESOURCE_METADATA_PATH
  }
}
