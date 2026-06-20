package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.AbstractMcpTest
import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.McpScreenshotBranchTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.model.key.Key
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager

class McpScreenshotBranchProtectionTest : AbstractMcpTest() {
  lateinit var testData: McpScreenshotBranchTestData
  lateinit var clientWithoutProtect: McpSyncClient
  lateinit var clientWithProtect: McpSyncClient

  @Autowired
  lateinit var transactionManager: PlatformTransactionManager

  @Autowired
  lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.BRANCHING)
    testData = McpScreenshotBranchTestData()
    testDataService.saveTestData(testData.root)
    clientWithoutProtect = createMcpClientWithPak(testData.pakWithoutProtectScope.encodedKey!!)
    clientWithProtect = createMcpClientWithPak(testData.pakWithProtectScope.encodedKey!!)
  }

  @AfterEach
  fun resetFeatures() {
    enabledFeaturesProvider.forceEnabled = null
  }

  @Test
  fun `add_key_screenshots on protected branch without protected scope is rejected and attaches nothing`() {
    val imageId = uploadImage(clientWithoutProtect)

    expectToolFailure {
      addScreenshot(clientWithoutProtect, "protected.existing", "main", imageId)
    }

    assertThat(screenshotsOf("protected.existing", "main")).isEmpty()
  }

  @Test
  fun `add_key_screenshots on protected branch with protected scope succeeds`() {
    val imageId = uploadImage(clientWithProtect)

    val result = addScreenshot(clientWithProtect, "protected.existing", "main", imageId)
    assertThat(result.isError).isFalse()

    assertThat(screenshotsOf("protected.existing", "main")).isNotEmpty()
  }

  @Test
  fun `add_key_screenshots on non-protected branch succeeds without protected scope`() {
    val imageId = uploadImage(clientWithoutProtect)

    val result = addScreenshot(clientWithoutProtect, "feature.existing", "feature", imageId)
    assertThat(result.isError).isFalse()

    assertThat(screenshotsOf("feature.existing", "feature")).isNotEmpty()
  }

  // create_keys silently skips keys that already exist but still attaches their screenshots, so the
  // branch check guards that pass independently of key creation — hence an already-existing key here.
  @Test
  fun `create_keys re-targeting an existing protected-branch key without protected scope attaches nothing`() {
    val imageId = uploadImage(clientWithoutProtect)

    expectToolFailure {
      createKeyWithScreenshot(clientWithoutProtect, "protected.existing", "main", imageId)
    }

    assertThat(screenshotsOf("protected.existing", "main")).isEmpty()
  }

  @Test
  fun `create_keys re-targeting an existing protected-branch key with protected scope attaches the screenshot`() {
    val imageId = uploadImage(clientWithProtect)

    val result = createKeyWithScreenshot(clientWithProtect, "protected.existing", "main", imageId)
    assertThat(result.isError).isFalse()

    assertThat(screenshotsOf("protected.existing", "main")).isNotEmpty()
  }

  private fun addScreenshot(
    client: McpSyncClient,
    keyName: String,
    branch: String,
    imageId: Long,
  ): McpSchema.CallToolResult =
    callTool(
      client,
      "add_key_screenshots",
      mapOf(
        "projectId" to testData.project.id,
        "branch" to branch,
        "keyScreenshots" to
          listOf(
            mapOf(
              "keyName" to keyName,
              "screenshots" to listOf(mapOf("uploadedImageId" to imageId)),
            ),
          ),
      ),
    )

  private fun createKeyWithScreenshot(
    client: McpSyncClient,
    keyName: String,
    branch: String,
    imageId: Long,
  ): McpSchema.CallToolResult =
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to testData.project.id,
        "branch" to branch,
        "keys" to
          listOf(
            mapOf(
              "name" to keyName,
              "screenshots" to listOf(mapOf("uploadedImageId" to imageId)),
            ),
          ),
      ),
    )

  private fun screenshotsOf(
    keyName: String,
    branch: String,
  ): List<*> =
    executeInNewTransaction(transactionManager) {
      val key: Key =
        keyService.find(testData.project.id, keyName, null, branch)
          ?: return@executeInNewTransaction emptyList<Any>()
      screenshotService.findAll(key)
    }
}
