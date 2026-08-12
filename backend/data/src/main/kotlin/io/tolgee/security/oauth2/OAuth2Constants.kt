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
  const val PROJECTS_CLAIM = "tg.prj"

  // Access-token claim carrying the id of the oauth2_authorization row that minted it, so the resolver can reject a
  // token whose authorization has been revoked (disconnect / logout-everywhere) without waiting for it to expire.
  const val AUTHORIZATION_ID_CLAIM = "tg.aid"

  /** [PROJECTS_CLAIM] sentinel: not narrowed to any project subset (still bounded by live permissions). */
  const val ALL_PROJECTS = "*"

  const val PROJECT_PARAM = "project"

  const val PROJECT_ATTRIBUTE = "tg.selected_project"

  const val BROWSER_EXTENSION_CLIENT_ID = "tolgee-browser-extension"
  const val CLI_CLIENT_ID = "tolgee-cli"

  /**
   * Client setting (space-delimited scope values) naming the scopes the consent screen locks as required. Stored as a
   * plain String, not a List, so it round-trips through the JdbcRegisteredClientRepository's allowlisted Jackson mapper.
   */
  const val REQUIRED_SCOPES_SETTING = "tolgee.required-scopes"
}
