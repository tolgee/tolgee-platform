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

/** The one resource server a grant's tokens may be presented to (RFC 8707). */
enum class OAuth2Audience(
  internal val pathPrefix: String?,
) {
  API(null),
  MCP(McpConstants.DEVELOPER_ENDPOINT_PATH),
  ;

  companion object {
    fun forRequestPath(path: String): OAuth2Audience = entries.firstOrNull { it.matches(path) } ?: API

    // The RFC 9728 document [OAuth2Resources.mcpResource] advertises one fixed resource, while enforcement derives
    // the audience from the path — so a sibling route resolving to MCP would be enforced against a resource it was
    // never advertised as.
    private fun OAuth2Audience.matches(path: String): Boolean {
      val prefix = pathPrefix ?: return false
      return path == prefix || path.startsWith("$prefix/")
    }
  }
}
