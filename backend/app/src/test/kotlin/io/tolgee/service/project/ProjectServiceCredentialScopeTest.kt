package io.tolgee.service.project

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.McpOAuthTestData
import io.tolgee.dtos.cacheable.ApiKeyDto
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.request.project.ProjectFilters
import io.tolgee.model.enums.Scope
import io.tolgee.security.authentication.TolgeeAuthentication
import io.tolgee.security.oauth2.OAuth2TokenCredentials
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder

class ProjectServiceCredentialScopeTest : AbstractSpringTest() {
  private lateinit var testData: McpOAuthTestData

  @BeforeEach
  fun setup() {
    testData = McpOAuthTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    SecurityContextHolder.clearContext()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a user session lists every permitted project`() {
    authenticateWith(credentials = null)

    assertThat(listedProjectIds()).isEqualTo(setOf(testData.project.id, testData.otherProject.id))
  }

  @Test
  fun `a project API key lists only its own project`() {
    authenticateWith(
      ApiKeyDto(
        id = 1,
        hash = "hash",
        expiresAt = null,
        projectId = testData.project.id,
        userAccountId = testData.user.id,
        scopes = setOf(Scope.KEYS_VIEW),
      ),
    )

    assertThat(listedProjectIds()).isEqualTo(setOf(testData.project.id))
  }

  @Test
  fun `an OAuth token for one project lists only that project`() {
    authenticateWith(OAuth2TokenCredentials(setOf(Scope.KEYS_VIEW), setOf(testData.otherProject.id)))

    assertThat(listedProjectIds()).isEqualTo(setOf(testData.otherProject.id))
  }

  @Test
  fun `an all-projects OAuth token lists every permitted project`() {
    authenticateWith(OAuth2TokenCredentials(setOf(Scope.KEYS_VIEW), null))

    assertThat(listedProjectIds()).isEqualTo(setOf(testData.project.id, testData.otherProject.id))
  }

  @Test
  fun `an OAuth token with no projects lists nothing`() {
    authenticateWith(OAuth2TokenCredentials(setOf(Scope.KEYS_VIEW), emptySet()))

    assertThat(listedProjectIds()).isEmpty()
  }

  @Test
  fun `a caller filter cannot widen the credential's project set`() {
    authenticateWith(OAuth2TokenCredentials(setOf(Scope.KEYS_VIEW), setOf(testData.project.id)))

    val filters = ProjectFilters().apply { filterId = listOf(testData.project.id, testData.otherProject.id) }

    assertThat(listedProjectIds(filters)).isEqualTo(setOf(testData.project.id))
  }

  private fun listedProjectIds(filters: ProjectFilters? = null): Set<Long> =
    projectService
      .findPermittedInOrganizationPaged(PageRequest.of(0, 100), null, filters = filters)
      .content
      .map { it.id }
      .toSet()

  private fun authenticateWith(credentials: Any?) {
    SecurityContextHolder.getContext().authentication =
      TolgeeAuthentication(
        credentials = credentials,
        deviceId = null,
        userAccount = UserAccountDto.fromEntity(testData.user),
        actingAsUserAccount = null,
        isReadOnly = false,
        isSuperToken = false,
      )
  }
}
