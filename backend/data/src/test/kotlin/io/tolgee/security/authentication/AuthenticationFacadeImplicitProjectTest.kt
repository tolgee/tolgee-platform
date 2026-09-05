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
    authenticate(apiKeyFor(PROJECT_ID))

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

  private fun apiKeyFor(projectId: Long) =
    ApiKeyDto(
      id = 1,
      hash = "hash",
      expiresAt = null,
      projectId = projectId,
      userAccountId = USER_ID,
      scopes = setOf(Scope.TRANSLATIONS_VIEW),
    )

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
