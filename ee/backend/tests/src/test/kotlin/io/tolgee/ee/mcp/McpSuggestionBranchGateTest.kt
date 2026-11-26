package io.tolgee.ee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class McpSuggestionBranchGateTest : AbstractMcpTest() {
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
  fun `list_keys_with_suggestions with branch fails when branching is not enabled`() {
    assertToolFails(
      client,
      "list_keys_with_suggestions",
      mapOf("projectId" to data.projectId, "languageTag" to "en", "branch" to "main"),
      expectedError = "feature_not_enabled",
    )
  }

  @Test
  fun `list_suggestions with branch fails when branching is not enabled`() {
    assertToolFails(
      client,
      "list_suggestions",
      mapOf(
        "projectId" to data.projectId,
        "keyName" to "some.key",
        "languageTag" to "en",
        "branch" to "main",
      ),
      expectedError = "feature_not_enabled",
    )
  }
}
