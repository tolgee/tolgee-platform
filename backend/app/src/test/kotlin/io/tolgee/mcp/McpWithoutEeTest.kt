package io.tolgee.mcp

import io.tolgee.mcp.tools.AbstractMcpTest
import io.tolgee.testing.WithoutEeTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@WithoutEeTest
class McpWithoutEeTest : AbstractMcpTest() {
  lateinit var data: McpTestData

  private val eePresent: Boolean by lazy {
    try {
      Class.forName("io.tolgee.ee.service.branching.BranchServiceImpl")
      true
    } catch (_: ClassNotFoundException) {
      false
    }
  }

  private val coreTools =
    setOf(
      "list_projects",
      "create_project",
      "get_project_language_statistics",
      "list_keys",
      "search_keys",
      "create_keys",
      "get_key",
      "update_key",
      "delete_keys",
      "list_languages",
      "create_language",
      "get_translations",
      "set_translation",
      "list_tags",
      "tag_keys",
      "list_namespaces",
      "get_batch_job_status",
      "machine_translate",
      "store_big_meta",
    )

  private val eeBranchTools =
    setOf(
      "list_branches",
      "create_branch",
      "delete_branch",
    )

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
  }

  @Test
  fun `core tools are always available`() {
    val client = createMcpClient(data.pat.token!!)
    val toolNames = client.listTools().tools().map { it.name() }.toSet()
    assertThat(toolNames).containsAll(coreTools)
  }

  @Test
  fun `branch tools are only available with EE module`() {
    val client = createMcpClient(data.pat.token!!)
    val toolNames = client.listTools().tools().map { it.name() }.toSet()
    if (eePresent) {
      assertThat(toolNames).containsAll(eeBranchTools)
    } else {
      assertThat(toolNames).doesNotContainAnyElementsOf(eeBranchTools)
    }
  }
}
