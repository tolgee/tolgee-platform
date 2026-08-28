package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.model.Pat
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpProjectToolsTest : AbstractMcpTest() {
  lateinit var data: McpPatTestData
  lateinit var client: McpSyncClient

  private val extraTestDataRoots = mutableListOf<TestDataBuilder>()

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
    client = createMcpClientWithPat(data.pat.token!!)
  }

  @AfterEach
  fun cleanTestData() {
    extraTestDataRoots.forEach { testDataService.cleanTestData(it) }
    extraTestDataRoots.clear()
    if (this::data.isInitialized) {
      testDataService.cleanTestData(data.testData.root)
    }
  }

  @Test
  fun `list_projects returns user projects`() {
    val json = callToolAndGetJson(client, "list_projects")
    assertThat(json["items"].isArray).isTrue()
    assertThat(json["items"].size()).isGreaterThanOrEqualTo(1)
    assertThat(json["page"].asInt()).isEqualTo(0)
    assertThat(json["totalPages"].asInt()).isGreaterThanOrEqualTo(1)
    assertThat(json["totalItems"].asLong()).isGreaterThanOrEqualTo(1)
    val projectNames = (0 until json["items"].size()).map { json["items"][it]["name"].asText() }
    assertThat(projectNames).contains("test_project")
  }

  @Test
  fun `list_projects with search filter`() {
    val json = callToolAndGetJson(client, "list_projects", mapOf("search" to "test_project"))
    assertThat(json["items"].isArray).isTrue()
    assertThat(json["items"].size()).isGreaterThanOrEqualTo(1)
    assertThat(json["items"][0]["name"].asText()).isEqualTo("test_project")
  }

  @Test
  fun `create_project creates a new project`() {
    val json =
      callToolAndGetJson(
        client,
        "create_project",
        createProjectArguments("New MCP Project"),
      )
    assertThat(json["id"]).isNotNull()
    assertThat(json["name"].asText()).isEqualTo("New MCP Project")
  }

  @Test
  fun `create_project grants the maintainer full access to the project it created`() {
    val maintainer = createUserWithPat("mcp_project_maintainer")
    executeInNewTransaction {
      organizationRoleService.grantRoleToUser(
        userAccountService.get(maintainer.userId),
        organizationService.get(data.organizationId),
        OrganizationRoleType.MAINTAINER,
      )
    }

    val json =
      callToolAndGetJson(
        createMcpClientWithPat(maintainer.token),
        "create_project",
        createProjectArguments("Maintainer MCP Project"),
      )

    val projectId = json["id"].asLong()
    executeInNewTransaction {
      assertThat(permissionService.getUserProjectPermission(projectId, maintainer.userId)?.type)
        .isEqualTo(ProjectPermissionType.MANAGE)
    }
  }

  @Test
  fun `create_project refuses a user with no role in the organization`() {
    val outsider = createUserWithPat("mcp_project_outsider")

    assertToolFails(
      createMcpClientWithPat(outsider.token),
      "create_project",
      createProjectArguments("Outsider MCP Project"),
      expectedError = "user_is_not_owner_or_maintainer_of_organization",
    )

    executeInNewTransaction {
      assertThat(projectService.findAllInOrganization(data.organizationId).map { it.name })
        .doesNotContain("Outsider MCP Project")
    }
  }

  @Test
  fun `get_project_language_statistics returns per-language stats`() {
    val json =
      callToolAndGetJson(
        client,
        "get_project_language_statistics",
        mapOf("projectId" to data.projectId),
      )
    assertThat(json.isArray).isTrue()
    assertThat(json.size()).isGreaterThan(0)
    assertThat(json[0].has("languageTag")).isTrue()
    assertThat(json[0].has("translatedPercentage")).isTrue()
    assertThat(json[0].has("reviewedPercentage")).isTrue()
    assertThat(json[0].has("untranslatedPercentage")).isTrue()
  }

  @Test
  fun `get_project_language_statistics auto-resolves projectId from PAK`() {
    val pakData = createTestDataWithPak()
    extraTestDataRoots.add(pakData.testData.root)
    val pakClient = createMcpClientWithPak(pakData.apiKey.encodedKey!!)

    val json = callToolAndGetJson(pakClient, "get_project_language_statistics")
    assertThat(json.isArray).isTrue()
    assertThat(json.size()).isGreaterThan(0)
    assertThat(json[0].has("languageTag")).isTrue()
  }

  private fun createUserWithPat(username: String): PatUser {
    var pat: Pat? = null
    val root = TestDataBuilder()
    val userBuilder = root.addUserAccount { this.username = username }
    userBuilder.build {
      addPat {
        description = "MCP test PAT"
        pat = this
      }
    }
    testDataService.saveTestData(root)
    extraTestDataRoots.add(root)
    return PatUser(userId = userBuilder.self.id, token = pat!!.token!!)
  }

  private data class PatUser(
    val userId: Long,
    val token: String,
  )

  private fun createProjectArguments(name: String) =
    mapOf(
      "name" to name,
      "organizationId" to data.organizationId,
      "languages" to
        listOf(
          mapOf("name" to "English", "tag" to "en"),
        ),
    )
}
