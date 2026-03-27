package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Base64

class McpScreenshotToolsTest : AbstractMcpTest() {
  lateinit var data: McpPakTestData
  lateinit var client: McpSyncClient

  companion object {
    // Minimal valid 1x1 red PNG
    private val MINIMAL_PNG_BYTES =
      byteArrayOf(
        // PNG signature
        0x89.toByte(),
        0x50,
        0x4E,
        0x47,
        0x0D,
        0x0A,
        0x1A,
        0x0A,
        // IHDR chunk
        0x00,
        0x00,
        0x00,
        0x0D,
        0x49,
        0x48,
        0x44,
        0x52,
        0x00,
        0x00,
        0x00,
        0x01,
        0x00,
        0x00,
        0x00,
        0x01,
        0x08,
        0x02,
        0x00,
        0x00,
        0x00,
        0x90.toByte(),
        0x77,
        0x53,
        0xDE.toByte(),
        0x00,
        // IDAT chunk
        0x00,
        0x00,
        0x00,
        0x0C,
        0x49,
        0x44,
        0x41,
        0x54,
        0x08,
        0xD7.toByte(),
        0x63,
        0xF8.toByte(),
        0xCF.toByte(),
        0xC0.toByte(),
        0x00,
        0x00,
        0x00,
        0x02,
        0x00,
        0x01,
        0xE2.toByte(),
        0x21,
        0xBC.toByte(),
        0x33,
        // IEND chunk
        0x00,
        0x00,
        0x00,
        0x00,
        0x49,
        0x45,
        0x4E,
        0x44,
        0xAE.toByte(),
        0x42,
        0x60,
        0x82.toByte(),
      )

    val MINIMAL_PNG_BASE64: String = Base64.getEncoder().encodeToString(MINIMAL_PNG_BYTES)
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

    val key = keyService.find(data.projectId, "positioned.key", null)
    assertThat(key).isNotNull()
    val screenshots = screenshotService.findAll(key!!)
    assertThat(screenshots).isNotEmpty()
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
}
