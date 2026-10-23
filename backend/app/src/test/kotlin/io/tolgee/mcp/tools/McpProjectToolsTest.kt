package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.development.testDataBuilder.data.ProjectStatsBranchingTestData
import io.tolgee.model.Pat
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpProjectToolsTest : AbstractMcpTest() {
  lateinit var data: McpPatTestData
  lateinit var client: McpSyncClient
  var branchedData: ProjectStatsBranchingTestData? = null

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
    client = createMcpClientWithPat(data.pat.token!!)
  }

  @Test
  fun `list_projects returns user projects`() {
    val json = callToolAndGetJson(client, "list_projects")
    assertThat(json["items"].isArray).isTrue()
    assertThat(json["items"].size()).isGreaterThanOrEqualTo(1)
    assertThat(json["page"].asInt()).isEqualTo(0)
    assertThat(json["totalPages"].asInt()).isGreaterThanOrEqualTo(1)
    assertThat(json["totalItems"].asLong()).isGreaterThanOrEqualTo(1)
    val projectNames = (0 until json["items"].size()).map { json["items"][it]["name"].asString() }
    assertThat(projectNames).contains("test_project")
  }

  @Test
  fun `list_projects with search filter`() {
    val json = callToolAndGetJson(client, "list_projects", mapOf("search" to "test_project"))
    assertThat(json["items"].isArray).isTrue()
    assertThat(json["items"].size()).isGreaterThanOrEqualTo(1)
    assertThat(json["items"][0]["name"].asString()).isEqualTo("test_project")
  }

  @Test
  fun `create_project creates a new project`() {
    val json =
      callToolAndGetJson(
        client,
        "create_project",
        mapOf(
          "name" to "New MCP Project",
          "organizationId" to data.organizationId,
          "languages" to
            listOf(
              mapOf("name" to "English", "tag" to "en"),
            ),
        ),
      )
    assertThat(json["id"]).isNotNull()
    assertThat(json["name"].asString()).isEqualTo("New MCP Project")
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
    val pakClient = createMcpClientWithPak(pakData.apiKey.encodedKey!!)

    val json = callToolAndGetJson(pakClient, "get_project_language_statistics")
    assertThat(json.isArray).isTrue()
    assertThat(json.size()).isGreaterThan(0)
    assertThat(json[0].has("languageTag")).isTrue()
  }

  @Test
  fun `get_project_language_statistics returns one entry per language when null-branch rows exist`() {
    var pat: Pat? = null
    val branched = ProjectStatsBranchingTestData()
    branchedData = branched
    branched.userAccountBuilder.build { addPat { pat = this } }
    testDataService.saveTestData(branched.root)
    val projectId = branched.project.id
    languageStatsService.refreshLanguageStats(projectId, null)
    languageStatsService.refreshLanguageStats(projectId, branched.mainBranch.id)

    val json =
      callToolAndGetJson(
        createMcpClientWithPat(pat!!.token!!),
        "get_project_language_statistics",
        mapOf("projectId" to projectId),
      )

    val tags = (0 until json.size()).map { json[it]["languageTag"].asString() }
    assertThat(tags).containsExactlyInAnyOrder("en", "de")
  }

  @AfterEach
  fun cleanupBranchedData() {
    branchedData?.let { testDataService.cleanTestData(it.root) }
  }
}
