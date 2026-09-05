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
import org.mockito.kotlin.any
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

  private fun authenticatedWithOAuth(credentials: OAuth2TokenCredentials) {
    stubCredentials(credentials)
  }

  private fun authenticatedWithoutOAuth() {
    stubCredentials(null)
  }

  private fun stubCredentials(credentials: OAuth2TokenCredentials?) {
    whenever(authenticationFacade.scopedCredential).thenReturn(credentials)
    whenever(authenticationFacade.isScopedCredential).thenReturn(credentials != null)
    whenever(authenticationFacade.isScopedCredentialFor(any())).thenReturn(credentials != null)
  }

  private fun userHasAccessTo(projectId: Long) {
    userHasScopeOn(projectId, Scope.TRANSLATIONS_VIEW)
  }

  private fun userHasScopeOn(
    projectId: Long,
    scope: Scope,
  ) {
    whenever(permissionService.getProjectPermissionScopesNoApiKey(projectId, 1L)).thenReturn(arrayOf(scope))
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
    authenticatedWithoutOAuth()

    assertThatCode { service().checkAnyProjectPermission(2L) }.doesNotThrowAnyException()
  }

  @Test
  fun `checkProjectPermission denies a scope the token doesn't cover even when the user holds it live`() {
    userHasScopeOn(2L, Scope.KEYS_EDIT)
    authenticatedWithOAuth(OAuth2TokenCredentials(setOf(Scope.TRANSLATIONS_EDIT), setOf(2L)))

    assertThatThrownBy { service().checkProjectPermission(2L, Scope.KEYS_EDIT, user) }
      .isInstanceOf(PermissionException::class.java)
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
