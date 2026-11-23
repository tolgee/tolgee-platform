package io.tolgee.ee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class McpTranslationBranchGateTest : AbstractMcpTest() {
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
  fun `set_translation with branch fails when branching is not enabled`() {
    assertToolFails(
      client,
      "set_translation",
      mapOf(
        "projectId" to data.projectId,
        "keyName" to "some.key",
        "translations" to mapOf("en" to "Hello"),
        "branch" to "main",
      ),
      expectedError = "feature_not_enabled",
    )
  }

  @Test
  fun `get_translations with branch fails when branching is not enabled`() {
    assertToolFails(
      client,
      "get_translations",
      mapOf(
        "projectId" to data.projectId,
        "keyName" to "some.key",
        "branch" to "main",
      ),
      expectedError = "feature_not_enabled",
    )
  }
}
