package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.ProjectCreationPermissionTestData
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.satisfies
import io.tolgee.model.UserAccount
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProjectsControllerCreatePermissionsTest : AuthorizedControllerTest() {
  lateinit var testData: ProjectCreationPermissionTestData

  @BeforeEach
  fun setup() {
    testData = ProjectCreationPermissionTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `creates the project for an organization owner without granting them an explicit project permission`() {
    assertCreatesWithoutExplicitPermission(testData.owner)
  }

  @Test
  fun `creates the project for a non-member server admin without granting them a project permission`() {
    assertCreatesWithoutExplicitPermission(testData.serverAdmin)
  }

  @Test
  fun `refuses project creation for a plain member of the organization`() {
    assertRefuses(testData.member)
  }

  @Test
  fun `refuses project creation for a user with no role in the organization`() {
    assertRefuses(testData.nonMember)
  }

  @Test
  fun `refuses project creation for a non-member server supporter`() {
    assertRefuses(testData.serverSupporter)
  }

  private fun assertCreatesWithoutExplicitPermission(user: UserAccount) {
    loginAsUser(user)
    performAuthPost("/v2/projects", createProjectRequest()).andIsOk.andAssertThatJson {
      node("id").asNumber().satisfies { id ->
        projectService.get(id.toLong()).let {
          it.organizationOwner.id.assert
            .isEqualTo(organizationId)
          permissionService.getUserProjectPermission(it.id, user.id).assert.isNull()
        }
      }
    }
  }

  private fun assertRefuses(user: UserAccount) {
    loginAsUser(user)
    performAuthPost("/v2/projects", createProjectRequest())
      .andIsForbidden
      .andHasErrorMessage(Message.USER_IS_NOT_OWNER_OR_MAINTAINER_OF_ORGANIZATION)
    projectService
      .findAllInOrganization(organizationId)
      .map { it.id }
      .assert
      .containsExactly(testData.project.id)
  }

  private val organizationId get() = testData.project.organizationOwner.id

  private fun createProjectRequest() =
    CreateProjectRequest(
      name = "New project",
      languages = listOf(LanguageRequest("English", "English", "en", "🇬🇧")),
      organizationId = organizationId,
    )
}
