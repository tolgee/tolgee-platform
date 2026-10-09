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

object OAuth2Constants {
  /** Project-selection sentinel: not narrowed to any project subset (still bounded by live permissions). */
  const val ALL_PROJECTS = "*"

  const val BROWSER_EXTENSION_CLIENT_ID = "tolgee-browser-extension"
  const val CLI_CLIENT_ID = "tolgee-cli"

  /**
   * Where the published Tolgee Tools extension receives the authorization code: the Chrome Web Store build, then the
   * Firefox add-on. Both ids are pinned in the extension's manifest, so these never change between releases.
   */
  val OFFICIAL_BROWSER_EXTENSION_REDIRECT_URIS =
    listOf(
      "https://hacnbapajkkfohnonhbmegojnddagfnj.chromiumapp.org/",
      "https://e262e73e8cbdd8d796b491acfa20a501bfc7b9c0.extensions.allizom.org/",
    )

  const val AUTHORIZE_PATH = "/oauth2/authorize"
  const val TOKEN_PATH = "/oauth2/token"
  const val REVOKE_PATH = "/oauth2/revoke"

  const val CONSENT_PAGE_PATH = "/oauth2/consent"

  const val AUTHORIZATION_SERVER_METADATA_PATH = "/.well-known/oauth-authorization-server"

  const val PROTECTED_RESOURCE_METADATA_PATH =
    "/.well-known/oauth-protected-resource${McpConstants.DEVELOPER_ENDPOINT_PATH}"
}
