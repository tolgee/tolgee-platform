package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager

class McpScreenshotToolsTest : AbstractMcpTest() {
  lateinit var data: McpPakTestData
  lateinit var client: McpSyncClient

  @Autowired
  lateinit var transactionManager: PlatformTransactionManager

  companion object {
    // Valid 1x1 RGB PNG with zlib compression
    const val MINIMAL_PNG_BASE64 =
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC"
  }

  @BeforeEach
  fun setup() {
    data = createTestDataWithPak()
    client = createMcpClientWithPak(data.apiKey.encodedKey!!)
  }

  @Test
  fun `upload_image returns image ID`() {
    val json =
      callToolAndGetJson(
        client,
        "upload_image",
        mapOf("image" to MINIMAL_PNG_BASE64),
      )
    assertThat(json["imageId"]).isNotNull()
    assertThat(json["imageId"].asLong()).isGreaterThan(0)
  }

  @Test
  fun `upload_image fails with invalid base64`() {
    val result =
      callTool(
        client,
        "upload_image",
        mapOf("image" to "not-valid-base64!!!"),
      )
    assertThat(result.isError).isTrue()
  }

  @Test
  fun `create_keys with screenshots associates uploaded image`() {
    // First upload an image
    val uploadJson =
      callToolAndGetJson(
        client,
        "upload_image",
        mapOf("image" to MINIMAL_PNG_BASE64),
      )
    val imageId = uploadJson["imageId"].asLong()

    // Create a key with the screenshot reference
    val createJson =
      callToolAndGetJson(
        client,
        "create_keys",
        mapOf(
          "projectId" to data.projectId,
          "keys" to
            listOf(
              mapOf(
                "name" to "screenshot.test.key",
                "translations" to mapOf("en" to "Test text"),
                "screenshots" to
                  listOf(
                    mapOf("uploadedImageId" to imageId),
                  ),
              ),
            ),
        ),
      )
    assertThat(createJson["created"].asBoolean()).isTrue()

    // Verify the screenshot was associated
    val key = keyService.find(data.projectId, "screenshot.test.key", null)
    assertThat(key).isNotNull()
    val screenshots = screenshotService.findAll(key!!)
    assertThat(screenshots).isNotEmpty()
  }

  @Test
  fun `create_keys with screenshots and positions stores position data`() {
    val uploadJson =
      callToolAndGetJson(
        client,
        "upload_image",
        mapOf("image" to MINIMAL_PNG_BASE64),
      )
    val imageId = uploadJson["imageId"].asLong()

    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf(
              "name" to "positioned.key",
              "translations" to mapOf("en" to "Positioned text"),
              "screenshots" to
                listOf(
                  mapOf(
                    "uploadedImageId" to imageId,
                    "positions" to
                      listOf(
                        mapOf("x" to 10, "y" to 20, "width" to 100, "height" to 30),
                      ),
                  ),
                ),
            ),
          ),
      ),
    )

    executeInNewTransaction(transactionManager) {
      val key = keyService.find(data.projectId, "positioned.key", null)
      assertThat(key).isNotNull()
      val screenshots = screenshotService.findAll(key!!)
      assertThat(screenshots).isNotEmpty()
      val positions = key.keyScreenshotReferences.flatMap { it.positions ?: emptyList() }
      assertThat(positions).isNotEmpty()
      val pos = positions.first()
      assertThat(pos.x).isGreaterThanOrEqualTo(0)
      assertThat(pos.y).isGreaterThanOrEqualTo(0)
      assertThat(pos.width).isGreaterThan(0)
      assertThat(pos.height).isGreaterThan(0)
    }
  }

  @Test
  fun `create_keys with same image for multiple keys`() {
    val uploadJson =
      callToolAndGetJson(
        client,
        "upload_image",
        mapOf("image" to MINIMAL_PNG_BASE64),
      )
    val imageId = uploadJson["imageId"].asLong()

    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf(
              "name" to "multi.key.one",
              "translations" to mapOf("en" to "First"),
              "screenshots" to listOf(mapOf("uploadedImageId" to imageId)),
            ),
            mapOf(
              "name" to "multi.key.two",
              "translations" to mapOf("en" to "Second"),
              "screenshots" to listOf(mapOf("uploadedImageId" to imageId)),
            ),
          ),
      ),
    )

    val key1 = keyService.find(data.projectId, "multi.key.one", null)
    val key2 = keyService.find(data.projectId, "multi.key.two", null)
    assertThat(key1).isNotNull()
    assertThat(key2).isNotNull()
    assertThat(screenshotService.findAll(key1!!)).isNotEmpty()
    assertThat(screenshotService.findAll(key2!!)).isNotEmpty()
  }

  @Test
  fun `add_key_screenshots associates screenshot with existing key`() {
    // Create a key first (without screenshots)
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "existing.key", "translations" to mapOf("en" to "Existing"))),
      ),
    )

    // Upload an image
    val uploadJson =
      callToolAndGetJson(
        client,
        "upload_image",
        mapOf("image" to MINIMAL_PNG_BASE64),
      )
    val imageId = uploadJson["imageId"].asLong()

    // Add screenshot to the existing key
    val json =
      callToolAndGetJson(
        client,
        "add_key_screenshots",
        mapOf(
          "projectId" to data.projectId,
          "keyScreenshots" to
            listOf(
              mapOf(
                "keyName" to "existing.key",
                "screenshots" to listOf(mapOf("uploadedImageId" to imageId)),
              ),
            ),
        ),
      )
    assertThat(json["success"].asBoolean()).isTrue()
    assertThat(json["keyCount"].asInt()).isEqualTo(1)

    val key = keyService.find(data.projectId, "existing.key", null)
    assertThat(key).isNotNull()
    assertThat(screenshotService.findAll(key!!)).isNotEmpty()
  }

  @Test
  fun `add_key_screenshots fails for non-existent key`() {
    val uploadJson =
      callToolAndGetJson(
        client,
        "upload_image",
        mapOf("image" to MINIMAL_PNG_BASE64),
      )
    val imageId = uploadJson["imageId"].asLong()

    val result =
      callTool(
        client,
        "add_key_screenshots",
        mapOf(
          "projectId" to data.projectId,
          "keyScreenshots" to
            listOf(
              mapOf(
                "keyName" to "nonexistent.key",
                "screenshots" to listOf(mapOf("uploadedImageId" to imageId)),
              ),
            ),
        ),
      )
    assertThat(result.isError).isTrue()
  }
}
