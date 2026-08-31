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

import io.tolgee.model.enums.Scope

/**
 * The OAuth scope values this server issues — Tolgee's own permission scopes, one for one. Server-wide, not per
 * client: what a client may hold is decided by the user on the consent screen, not by its registration.
 */
object OAuth2Scopes {
  val SUPPORTED: List<String> = Scope.entries.map { it.value }

  private val BY_VALUE = Scope.entries.associateBy { it.value }
  private val BY_NAME = Scope.entries.associateBy { it.name }

  /** RFC 6749 §3.3: scope is a space-delimited, order-independent list. */
  fun splitScopeString(raw: String?): List<String> = raw.orEmpty().split(" ").filter { it.isNotBlank() }

  fun isSupported(scope: String): Boolean = scope in BY_VALUE

  fun find(scope: String): Scope? = BY_VALUE[scope]

  /**
   * Grants persist [Scope.name], not [Scope.value], for the same reason `ApiKey.scopesEnum` does: `value` is the wire
   * spelling and is a `var`, so renaming it for API cosmetics would silently narrow every live grant that held it.
   */
  fun findByName(name: String): Scope? = BY_NAME[name]
}
