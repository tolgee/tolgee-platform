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

package io.tolgee.service.security

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.oauth2.OAuth2TokenCredentials
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * checkAnyProjectPermission must apply the OAuth token's project-set ceiling: even when the user has live access to a
 * project, a token not bound to it must be denied (so it can't act past its project-set binding).
 */
class SecurityServiceOAuthNarrowingTest {
  private val user =
    mock<UserAccountDto> {
      on { id } doReturn 1L
      on { role } doReturn UserAccount.Role.USER
    }
  private val authenticationFacade = mock<AuthenticationFacade> { on { authenticatedUserOrNull } doReturn user }
  private val permissionService = mock<PermissionService>()

  private fun service(): SecurityService =
    SecurityService(authenticationFacade, mock(), mock(), mock(), mock()).apply {
      permissionService = this@SecurityServiceOAuthNarrowingTest.permissionService
    }

  private fun userHasAccessTo(projectId: Long) {
    whenever(permissionService.getProjectPermissionScopesNoApiKey(projectId, 1L))
      .thenReturn(arrayOf(Scope.TRANSLATIONS_VIEW))
  }

  @Test
  fun `denies an OAuth token not covering the project even when the user has access`() {
    userHasAccessTo(2L)
    whenever(authenticationFacade.oauthTokenCredentials)
      .thenReturn(OAuth2TokenCredentials(setOf(Scope.TRANSLATIONS_VIEW), setOf(1L)))

    assertThatThrownBy { service().checkAnyProjectPermission(2L) }.isInstanceOf(PermissionException::class.java)
  }

  @Test
  fun `allows an OAuth token covering the project`() {
    userHasAccessTo(2L)
    whenever(authenticationFacade.oauthTokenCredentials)
      .thenReturn(OAuth2TokenCredentials(setOf(Scope.TRANSLATIONS_VIEW), setOf(2L)))

    assertThatCode { service().checkAnyProjectPermission(2L) }.doesNotThrowAnyException()
  }

  @Test
  fun `allows a non-OAuth caller with project access`() {
    userHasAccessTo(2L)
    whenever(authenticationFacade.oauthTokenCredentials).thenReturn(null)

    assertThatCode { service().checkAnyProjectPermission(2L) }.doesNotThrowAnyException()
  }
}
