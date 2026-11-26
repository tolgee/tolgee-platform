package io.tolgee.ee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class McpKeyBranchGateTest : AbstractMcpTest() {
  lateinit var data: McpPakTestData
  lateinit var client: McpSyncClient

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    data = createTestDataWithPak()
    enabledFeaturesProvider.forceEnabled = emptySet()
    client = createMcpClientWithPak(data.apiKey.encodedKey!!)
  }

  @AfterEach
  fun tearDown() {
    enabledFeaturesProvider.forceEnabled = null
    testDataService.cleanTestData(data.testData.root)
  }

  @Test
  fun `search_keys with branch fails when branching is not enabled`() {
    assertToolFails(
      client,
      "search_keys",
      mapOf("projectId" to data.projectId, "query" to "anything", "branch" to "main"),
      expectedError = "feature_not_enabled",
    )
  }

  @Test
  fun `list_keys with branch fails when branching is not enabled`() {
    assertToolFails(
      client,
      "list_keys",
      mapOf("projectId" to data.projectId, "branch" to "main"),
      expectedError = "feature_not_enabled",
    )
  }

  @Test
  fun `get_key with branch fails when branching is not enabled`() {
    assertToolFails(
      client,
      "get_key",
      mapOf("projectId" to data.projectId, "keyName" to "some.key", "branch" to "main"),
      expectedError = "feature_not_enabled",
    )
  }

  @Test
  fun `delete_keys with branch fails when branching is not enabled`() {
    assertToolFails(
      client,
      "delete_keys",
      mapOf("projectId" to data.projectId, "keyNames" to listOf("some.key"), "branch" to "main"),
      expectedError = "feature_not_enabled",
    )
  }

  @Test
  fun `create_keys with branch fails when branching is not enabled`() {
    assertToolFails(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "gated.key")),
        "branch" to "main",
      ),
      expectedError = "feature_not_enabled",
    )
  }
}
