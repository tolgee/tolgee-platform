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

class AuthenticationFacadeScopedCredentialTest {
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
