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

package io.tolgee.security.authentication

import io.tolgee.dtos.cacheable.ApiKeyDto
import io.tolgee.dtos.cacheable.PatDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.enums.Scope
import io.tolgee.security.oauth2.OAuth2TokenCredentials
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.security.core.context.SecurityContextHolder

class AuthenticationFacadeImplicitProjectTest {
  private val facade = AuthenticationFacade(mock(), mock(), mock())

  @AfterEach
  fun clear() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `a project API key names its own project`() {
    authenticate(mock<ApiKeyDto> { on { projectId } doReturn PROJECT_ID })

    facade.implicitProjectId.assert.isEqualTo(PROJECT_ID)
  }

  @Test
  fun `an OAuth token bound to one project names it`() {
    authenticate(oauthBoundTo(setOf(PROJECT_ID)))

    facade.implicitProjectId.assert.isEqualTo(PROJECT_ID)
  }

  @Test
  fun `an OAuth token bound to several projects names none of them`() {
    authenticate(oauthBoundTo(setOf(PROJECT_ID, PROJECT_ID + 1)))

    facade.implicitProjectId.assert.isNull()
  }

  @Test
  fun `an all-projects OAuth token names no project`() {
    authenticate(oauthBoundTo(null))

    facade.implicitProjectId.assert.isNull()
  }

  @Test
  fun `a PAT names no project`() {
    authenticate(mock<PatDto>())

    facade.implicitProjectId.assert.isNull()
  }

  @Test
  fun `a webapp JWT names no project`() {
    authenticate(credentials = null)

    facade.implicitProjectId.assert.isNull()
  }

  private fun oauthBoundTo(projectIds: Set<Long>?) =
    OAuth2TokenCredentials(scopes = setOf(Scope.TRANSLATIONS_VIEW), projectIds = projectIds)

  private fun authenticate(credentials: Any?) {
    SecurityContextHolder.getContext().authentication =
      TolgeeAuthentication(
        credentials = credentials,
        deviceId = null,
        userAccount = mock<UserAccountDto> { on { id } doReturn USER_ID },
        actingAsUserAccount = null,
        isReadOnly = false,
        isSuperToken = false,
      )
  }

  companion object {
    private const val USER_ID = 42L
    private const val PROJECT_ID = 7L
  }
}
