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

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.handler.MappedInterceptor

/**
 * The paths [ProjectAuthorizationInterceptor] is registered for, and therefore the only ones where a credential's
 * project permissions can be applied — a handler outside them is never narrowed, whatever it is annotated with.
 * Matching a path is necessary but not sufficient; [ProjectAuthorizationInterceptor] owns the rest of the answer.
 */
object ProjectScopedEndpoints {
  val PATTERNS = arrayOf("/v2/projects/**", "/api/project/**", "/api/repository/**")

  // Must stay the same matcher WebSecurityConfig registers these patterns with.
  private val mappedPatterns = MappedInterceptor(PATTERNS, null, object : HandlerInterceptor {})

  fun matches(request: HttpServletRequest): Boolean = mappedPatterns.matches(request)
}
