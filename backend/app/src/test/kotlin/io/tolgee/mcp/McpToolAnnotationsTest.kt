package io.tolgee.mcp

import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpToolAnnotationsTest : AbstractMcpTest() {
  private lateinit var data: McpPatTestData

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(data.testData.root)
  }

  @Test
  fun `every tool has a title and declares whether it is read-only or destructive`() {
    val tools = listAllTools()

    assertThat(tools).isNotEmpty
    tools.forEach { tool ->
      assertThat(tool.title()).describedAs("title of ${tool.name()}").isNotBlank()
      val annotations = tool.annotations()
      assertThat(annotations).describedAs("annotations of ${tool.name()}").isNotNull
      assertThat(annotations.readOnlyHint()).describedAs("readOnlyHint of ${tool.name()}").isNotNull
      assertThat(annotations.destructiveHint()).describedAs("destructiveHint of ${tool.name()}").isNotNull
      assertThat(annotations.readOnlyHint() && annotations.destructiveHint())
        .describedAs("${tool.name()} is both read-only and destructive")
        .isFalse()
    }
  }

  @Test
  fun `read tools are read-only and delete tools are destructive`() {
    val tools = listAllTools().associateBy { it.name() }

    assertThat(tools["list_keys"]!!.annotations().readOnlyHint()).isTrue()
    assertThat(tools["create_keys"]!!.annotations().readOnlyHint()).isFalse()
    assertThat(tools["create_keys"]!!.annotations().destructiveHint()).isFalse()
    assertThat(tools["delete_keys"]!!.annotations().destructiveHint()).isTrue()
  }

  /** A personal access token gets the full list; OAuth and anonymous clients get a shorter one. */
  private fun listAllTools(): List<McpSchema.Tool> = createMcpClientWithPat(data.pat.token!!).listTools().tools()
}
