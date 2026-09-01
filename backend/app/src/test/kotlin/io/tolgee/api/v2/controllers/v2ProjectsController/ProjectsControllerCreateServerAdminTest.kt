package io.tolgee.api.v2.controllers.v2ProjectsController

import io.tolgee.development.testDataBuilder.data.ServerAdminProjectCreationTestData
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.project.CreateProjectRequest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.satisfies
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProjectsControllerCreateServerAdminTest : AuthorizedControllerTest() {
  lateinit var testData: ServerAdminProjectCreationTestData

  @BeforeEach
  fun setup() {
    testData = ServerAdminProjectCreationTestData()
    testDataService.saveTestData(testData.root)
    loginAsUser(testData.serverAdmin)
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `creates the project for a non-member server admin without granting them a project permission`() {
    val request =
      CreateProjectRequest(
        name = "Admin created",
        languages = listOf(LanguageRequest("English", "English", "en", "🇬🇧")),
        organizationId = testData.project.organizationOwner.id,
      )

    performAuthPost("/v2/projects", request).andIsOk.andAssertThatJson {
      node("id").asNumber().satisfies { id ->
        projectService.get(id.toLong()).let {
          it.organizationOwner.id.assert
            .isEqualTo(testData.project.organizationOwner.id)
          permissionService.getUserProjectPermission(it.id, testData.serverAdmin.id).assert.isNull()
        }
      }
    }
  }
}
