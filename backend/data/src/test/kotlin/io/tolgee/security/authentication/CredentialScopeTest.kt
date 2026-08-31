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

class CredentialScopeTest {
  private val facade = AuthenticationFacade(mock(), mock(), mock())

  @AfterEach
  fun clear() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `a thread with no security context - batch, @Async - is not scoped`() {
    facade.isScopedCredentialFor(USER_ID).assert.isFalse()
  }

  @Test
  fun `a webapp JWT is the user acting directly`() {
    authenticate(credentials = null)
    facade.isScopedCredentialFor(USER_ID).assert.isFalse()
  }

  @Test
  fun `a PAT carries the user's full authority`() {
    authenticate(credentials = mock<PatDto>())
    facade.isScopedCredentialFor(USER_ID).assert.isFalse()
  }

  @Test
  fun `a project API key is scoped`() {
    authenticate(credentials = mock<ApiKeyDto>())
    facade.isScopedCredentialFor(USER_ID).assert.isTrue()
  }

  @Test
  fun `an OAuth token is scoped`() {
    authenticate(credentials = OAuth2TokenCredentials(setOf(Scope.TRANSLATIONS_VIEW), null))
    facade.isScopedCredentialFor(USER_ID).assert.isTrue()
  }

  @Test
  fun `a scoped credential narrows only its own holder`() {
    authenticate(credentials = mock<ApiKeyDto>())

    facade.isScopedCredentialFor(USER_ID).assert.isTrue()
    facade.isScopedCredentialFor(USER_ID + 1).assert.isFalse()
  }

  private fun authenticate(credentials: Any?) {
    val user = mock<UserAccountDto> { on { id } doReturn USER_ID }
    SecurityContextHolder.getContext().authentication =
      TolgeeAuthentication(
        credentials = credentials,
        deviceId = null,
        userAccount = user,
        actingAsUserAccount = null,
        isReadOnly = false,
        isSuperToken = false,
      )
  }

  companion object {
    private const val USER_ID = 42L
  }
}
