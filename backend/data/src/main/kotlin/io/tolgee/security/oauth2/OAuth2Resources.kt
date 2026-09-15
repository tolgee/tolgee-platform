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
 * The RFC 8707 resource identifiers this server accepts and the audience each selects.
 *
 * [mcpResource] must stay byte-identical to the `resource` the RFC 9728 protected-resource document advertises:
 * a spec-following MCP client echoes that document's value here, and comparison is exact.
 */
@Component
class OAuth2Resources(
  private val issuerResolver: OAuth2IssuerResolver,
) {
  val apiResource: String get() = issuerResolver.issuerUrl

  val mcpResource: String get() = issuerResolver.issuerUrl + McpConstants.DEVELOPER_ENDPOINT_PATH

  fun audienceFor(resource: String?): OAuth2Audience {
    if (resource == null) return OAuth2Audience.API
    if (resource == apiResource) return OAuth2Audience.API
    if (resource == mcpResource) return OAuth2Audience.MCP
    throw OAuth2Error(OAuth2Error.INVALID_TARGET, "unknown resource")
  }
}
