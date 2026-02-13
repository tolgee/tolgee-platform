package io.tolgee.mcp

import io.tolgee.AbstractMcpTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope
import org.junit.jupiter.api.Test

class McpTokenTypeEnforcementTest : AbstractMcpTest() {
  @Test
  fun `PAK cannot call list_projects`() {
    val pakData = createTestDataWithPak()
    val client = createMcpClientWithPak(pakData.apiKey.encodedKey!!)

    assertToolFails(client, "list_projects", expectedError = "pak_access_not_allowed")
  }

  @Test
  fun `PAK cannot call create_project`() {
    val pakData = createTestDataWithPak()
    val client = createMcpClientWithPak(pakData.apiKey.encodedKey!!)

    assertToolFails(
      client,
      "create_project",
      mapOf(
        "name" to "Should Fail",
        "organizationId" to pakData.organizationId,
        "languages" to listOf(mapOf("name" to "English", "tag" to "en")),
      ),
      expectedError = "pak_access_not_allowed",
    )
  }

  @Test
  fun `PAT can call list_projects`() {
    val patData = createTestDataWithPat()
    val client = createMcpClientWithPat(patData.pat.token!!)

    assertToolSucceeds(client, "list_projects")
  }

  @Test
  fun `PAT can call project-scoped tools with explicit projectId`() {
    val patData = createTestDataWithPat()
    val client = createMcpClientWithPat(patData.pat.token!!)

    assertToolSucceeds(client, "list_keys", mapOf("projectId" to patData.projectId))
    assertToolSucceeds(client, "list_languages", mapOf("projectId" to patData.projectId))
  }

  @Test
  fun `PAK rejected when accessing different project`() {
    // Build test data with two projects under the same user/org:
    // PAK is bound to project A, but we'll try to access project B
    var apiKey: io.tolgee.model.ApiKey? = null
    val base = BaseTestData(userName = "cross_project_user", projectName = "project_a")
    base.projectBuilder.build {
      addApiKey {
        key = "cross_project_pak"
        scopesEnum = Scope.entries.toMutableSet()
        userAccount = base.userAccountBuilder.self
        apiKey = this
      }
    }
    val projectB =
      base.root.addProject {
        name = "project_b"
        organizationOwner = base.projectBuilder.self.organizationOwner
      }.build {
        addPermission {
          user = base.user
          type = ProjectPermissionType.MANAGE
        }
        addLanguage {
          name = "English"
          tag = "en"
        }
      }
    testDataService.saveTestData(base.root)

    val client = createMcpClientWithPak(apiKey!!.encodedKey!!)

    assertToolFails(
      client,
      "list_keys",
      mapOf("projectId" to projectB.self.id),
      expectedError = "pak_created_for_different_project",
    )
  }
}
