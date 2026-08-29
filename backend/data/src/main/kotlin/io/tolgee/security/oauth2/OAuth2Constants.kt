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

object OAuth2Constants {
  /** Project-selection sentinel: not narrowed to any project subset (still bounded by live permissions). */
  const val ALL_PROJECTS = "*"

  const val BROWSER_EXTENSION_CLIENT_ID = "tolgee-browser-extension"
  const val CLI_CLIENT_ID = "tolgee-cli"

  const val AUTHORIZE_PATH = "/oauth2/authorize"
  const val TOKEN_PATH = "/oauth2/token"

  /** SPA route the authorization endpoint hands the browser to. */
  const val CONSENT_PAGE_PATH = "/oauth2/consent"

  const val MCP_RESOURCE_PATH = "/mcp/developer"
  const val PROTECTED_RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource$MCP_RESOURCE_PATH"

  /** RFC 6749 §3.3: scope is a space-delimited, order-independent list. */
  fun splitScopeString(raw: String?): List<String> = raw.orEmpty().split(" ").filter { it.isNotBlank() }
}
