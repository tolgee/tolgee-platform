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

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.util.ServletRequestPathUtils

/**
 * The OAuth gate admits a token on these paths because [ProjectAuthorizationInterceptor] is registered for them and
 * will narrow it. A path this answers true for that the interceptor does not actually run on would be admitted and
 * never narrowed.
 */
class ProjectScopedEndpointsTest {
  @Test
  fun `project-scoped paths are matched`() {
    matches("/v2/projects").assert.isTrue()
    matches("/v2/projects/1/keys").assert.isTrue()
    matches("/v2/projects/1/translations/2/comments/3").assert.isTrue()
    matches("/api/project/export").assert.isTrue()
    matches("/api/repository/legacy").assert.isTrue()
  }

  @Test
  fun `paths outside them are not`() {
    matches("/v2/user").assert.isFalse()
    matches("/v2/user-tasks").assert.isFalse()
    matches("/v2/organizations/1").assert.isFalse()
    matches("/v2/api-keys/current-permissions").assert.isFalse()
    matches("/v2/projectsX").assert.isFalse()
    matches("/v2/project").assert.isFalse()
    matches("/mcp/developer").assert.isFalse()
  }

  private fun matches(path: String): Boolean {
    val request = MockHttpServletRequest("GET", path)
    ServletRequestPathUtils.parseAndCache(request)
    return ProjectScopedEndpoints.matches(request)
  }
}
