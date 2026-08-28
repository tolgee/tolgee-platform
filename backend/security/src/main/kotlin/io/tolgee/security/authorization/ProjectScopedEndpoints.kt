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

package io.tolgee.security.authorization

import org.springframework.util.AntPathMatcher

/**
 * The paths [ProjectAuthorizationInterceptor] is registered for, and therefore the only ones where a credential's
 * project permissions can be applied.
 *
 * A matching path is necessary but not sufficient, so this is one half of "will this request be narrowed?", not the
 * whole answer: [IsGlobalRoute] makes the interceptor return before it narrows anything, whatever the path.
 *
 * Declaring `@RequiresProjectPermissions` or `@UseDefaultPermissions` is a different question again: a handler outside
 * these paths can carry one and never be narrowed, because the interceptor is registered by path and simply never runs
 * for it. `/v2/user-tasks` is exactly that — annotated, unnarrowed, and returning every task the user has in every
 * project.
 */
object ProjectScopedEndpoints {
  val PATTERNS = arrayOf("/v2/projects/**", "/api/project/**", "/api/repository/**")

  private val matcher = AntPathMatcher()

  fun matches(path: String): Boolean = PATTERNS.any { matcher.match(it, path) }
}
