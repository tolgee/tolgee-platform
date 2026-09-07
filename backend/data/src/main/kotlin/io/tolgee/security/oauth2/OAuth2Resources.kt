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
import org.springframework.stereotype.Component

/**
 * RFC 8707: maps the `resource` parameter to the audience a grant gets bound to. Tolgee publishes exactly two
 * resource identifiers — the issuer origin (REST API) and the MCP canonical URI advertised by RFC 9728 metadata.
 */
@Component
class OAuth2Resources(
  private val issuerResolver: OAuth2IssuerResolver,
) {
  fun audienceFor(resource: String?): OAuth2Audience {
    if (resource == null) return OAuth2Audience.API
    val issuer = issuerResolver.issuerUrl
    if (resource == issuer) return OAuth2Audience.API
    if (resource == issuer + McpConstants.DEVELOPER_ENDPOINT_PATH) return OAuth2Audience.MCP
    throw OAuth2Error(OAuth2Error.INVALID_TARGET, "unknown resource")
  }
}
