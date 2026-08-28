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

import io.tolgee.constants.Message
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
 * The OAuth token's scope ∩ project-set ceiling must be applied on every project-permission check — both the
 * project-set gate on checkAnyProjectPermission and the scope+project-set gate on checkProjectPermission (the latter is
 * the one nested/secondary checks like KEYS_EDIT during a translation write hit, past the endpoint interceptor). Even
 * when the user holds the permission live, a narrower token must be denied so it can't act past what was consented.
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

  /**
   * In production `isScopedCredential` is `isProjectApiKeyAuth || isOAuthTokenAuth`, so with an OAuth token present it
   * is necessarily true. Stubbing the credentials without it lets the mock report a state that cannot occur, and the
   * permission lookups would then be matched for the wrong `asScopedCredential` value — the tests would pass without
   * exercising the narrowing they are named for.
   */
  private fun authenticatedWithOAuth(credentials: OAuth2TokenCredentials?) {
    whenever(authenticationFacade.oauthTokenCredentials).thenReturn(credentials)
    whenever(authenticationFacade.isScopedCredential).thenReturn(credentials != null)
  }

  private fun userHasAccessTo(projectId: Long) {
    userHasScopeOn(projectId, Scope.TRANSLATIONS_VIEW)
  }

  private fun userHasScopeOn(
    projectId: Long,
    scope: Scope,
  ) {
    // Stubbed for both values: an OAuth-authenticated call passes asScopedCredential = true, a plain one false.
    whenever(permissionService.getProjectPermissionScopesNoApiKey(projectId, 1L, false)).thenReturn(arrayOf(scope))
    whenever(permissionService.getProjectPermissionScopesNoApiKey(projectId, 1L, true)).thenReturn(arrayOf(scope))
  }

  @Test
  fun `denies an OAuth token not covering the project even when the user has access`() {
    userHasAccessTo(2L)
    authenticatedWithOAuth(OAuth2TokenCredentials(setOf(Scope.TRANSLATIONS_VIEW), setOf(1L)))

    assertThatThrownBy { service().checkAnyProjectPermission(2L) }.isInstanceOf(PermissionException::class.java)
  }

  @Test
  fun `allows an OAuth token covering the project`() {
    userHasAccessTo(2L)
    authenticatedWithOAuth(OAuth2TokenCredentials(setOf(Scope.TRANSLATIONS_VIEW), setOf(2L)))

    assertThatCode { service().checkAnyProjectPermission(2L) }.doesNotThrowAnyException()
  }

  @Test
  fun `allows a non-OAuth caller with project access`() {
    userHasAccessTo(2L)
    authenticatedWithOAuth(null)

    assertThatCode { service().checkAnyProjectPermission(2L) }.doesNotThrowAnyException()
  }

  @Test
  fun `checkProjectPermission denies a scope the token doesn't cover even when the user holds it live`() {
    // The user has keys.edit live, but the token was only granted translations.edit — the nested KEYS_EDIT check
    // (past the endpoint interceptor) must be denied so a narrow token can't create/delete keys.
    userHasScopeOn(2L, Scope.KEYS_EDIT)
    authenticatedWithOAuth(OAuth2TokenCredentials(setOf(Scope.TRANSLATIONS_EDIT), setOf(2L)))

    assertThatThrownBy { service().checkProjectPermission(2L, Scope.KEYS_EDIT, user) }
      .isInstanceOf(PermissionException::class.java)
      // A covered project but a missing scope is a scope error, not a project-access error.
      .hasFieldOrPropertyWithValue("code", Message.OPERATION_NOT_PERMITTED.code)
  }

  @Test
  fun `checkProjectPermission denies a token bound to a different project even when scope and live access cover it`() {
    userHasScopeOn(2L, Scope.KEYS_EDIT)
    authenticatedWithOAuth(OAuth2TokenCredentials(setOf(Scope.KEYS_EDIT), setOf(1L)))

    assertThatThrownBy { service().checkProjectPermission(2L, Scope.KEYS_EDIT, user) }
      .isInstanceOf(PermissionException::class.java)
      // A project outside the token set is a project-access denial, not a missing-scope error.
      .hasFieldOrPropertyWithValue("code", Message.USER_HAS_NO_PROJECT_ACCESS.code)
  }

  @Test
  fun `checkProjectPermission allows a token covering both the scope and the project`() {
    userHasScopeOn(2L, Scope.KEYS_EDIT)
    authenticatedWithOAuth(OAuth2TokenCredentials(setOf(Scope.KEYS_EDIT), setOf(2L)))

    assertThatCode { service().checkProjectPermission(2L, Scope.KEYS_EDIT, user) }.doesNotThrowAnyException()
  }
}
