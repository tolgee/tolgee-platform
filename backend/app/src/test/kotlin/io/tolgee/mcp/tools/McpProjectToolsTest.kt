package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpProjectToolsTest : AbstractMcpTest() {
  lateinit var data: McpTestData
  lateinit var client: McpSyncClient

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
    client = createMcpClient(data.pat.token!!)
  }

  @Test
  fun `list_projects returns user projects`() {
    val json = callToolAndGetJson(client, "list_projects")
    assertThat(json.isArray).isTrue()
    assertThat(json.size()).isGreaterThanOrEqualTo(1)
    val projectNames = (0 until json.size()).map { json[it]["name"].asText() }
    assertThat(projectNames).contains("test_project")
  }

  @Test
  fun `list_projects with search filter`() {
    val json = callToolAndGetJson(client, "list_projects", mapOf("search" to "test_project"))
    assertThat(json.isArray).isTrue()
    assertThat(json.size()).isGreaterThanOrEqualTo(1)
    assertThat(json[0]["name"].asText()).isEqualTo("test_project")
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
    assertThat(json["name"].asText()).isEqualTo("New MCP Project")
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
    if (json.size() > 0) {
      assertThat(json[0].has("languageTag")).isTrue()
      assertThat(json[0].has("translatedPercentage")).isTrue()
      assertThat(json[0].has("reviewedPercentage")).isTrue()
      assertThat(json[0].has("untranslatedPercentage")).isTrue()
    }
  }
}
