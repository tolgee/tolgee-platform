package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.AbstractMcpTest
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.PlatformTransactionManager

@TestPropertySource(properties = ["tolgee.max-screenshots-per-key=2"])
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
    assertThat(json["uploadedImageId"]).isNotNull()
    assertThat(json["uploadedImageId"].asLong()).isGreaterThan(0)
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
    val uploadJson =
      callToolAndGetJson(
        client,
        "upload_image",
        mapOf("image" to MINIMAL_PNG_BASE64),
      )
    val imageId = uploadJson["uploadedImageId"].asLong()

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
    val imageId = uploadJson["uploadedImageId"].asLong()

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
      // The 1x1 source PNG yields a 1.0 scaling ratio, so coordinates round-trip unchanged.
      assertThat(pos.x).isEqualTo(10)
      assertThat(pos.y).isEqualTo(20)
      assertThat(pos.width).isEqualTo(100)
      assertThat(pos.height).isEqualTo(30)
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
    val imageId = uploadJson["uploadedImageId"].asLong()

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
    val screenshots1 = screenshotService.findAll(key1!!)
    val screenshots2 = screenshotService.findAll(key2!!)
    assertThat(screenshots1).hasSize(1)
    assertThat(screenshots2).hasSize(1)
    assertThat(screenshots1.first().id).isEqualTo(screenshots2.first().id)
  }

  @Test
  fun `create_keys merges positions when the same image is referenced twice for one key`() {
    val imageId = uploadImage()

    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf(
              "name" to "dup.key",
              "screenshots" to
                listOf(
                  mapOf(
                    "uploadedImageId" to imageId,
                    "positions" to listOf(mapOf("x" to 1, "y" to 2, "width" to 3, "height" to 4)),
                  ),
                  mapOf(
                    "uploadedImageId" to imageId,
                    "positions" to listOf(mapOf("x" to 5, "y" to 6, "width" to 7, "height" to 8)),
                  ),
                ),
            ),
          ),
      ),
    )

    executeInNewTransaction(transactionManager) {
      val key = keyService.find(data.projectId, "dup.key", null)
      assertThat(key).isNotNull()
      // One reference (the image is deduplicated) but both DTOs' positions are preserved.
      assertThat(screenshotService.findAll(key!!)).hasSize(1)
      val positions = key!!.keyScreenshotReferences.flatMap { it.positions ?: emptyList() }
      assertThat(positions).hasSize(2)
    }
  }

  @Test
  fun `create_keys attaches a screenshot to a key in a namespace`() {
    val imageId = uploadImage()

    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to
          listOf(
            mapOf(
              "name" to "ns.key",
              "namespace" to "my-ns",
              "screenshots" to listOf(mapOf("uploadedImageId" to imageId)),
            ),
          ),
      ),
    )

    val key = keyService.find(data.projectId, "ns.key", "my-ns")
    assertThat(key).isNotNull()
    assertThat(screenshotService.findAll(key!!)).hasSize(1)
  }

  @Test
  fun `create_keys with a blank namespace still attaches the screenshot`() {
    val imageId = uploadImage()

    val result =
      callTool(
        client,
        "create_keys",
        mapOf(
          "projectId" to data.projectId,
          "keys" to
            listOf(
              mapOf(
                "name" to "blank.ns.key",
                "namespace" to "",
                "screenshots" to listOf(mapOf("uploadedImageId" to imageId)),
              ),
            ),
        ),
      )

    assertThat(result.isError).isFalse()
    val key = keyService.find(data.projectId, "blank.ns.key", null)
    assertThat(key).isNotNull()
    assertThat(screenshotService.findAll(key!!)).hasSize(1)
  }

  @Test
  fun `add_key_screenshots rejects exceeding the per-key screenshot limit`() {
    callTool(
      client,
      "create_keys",
      mapOf("projectId" to data.projectId, "keys" to listOf(mapOf("name" to "limited.key"))),
    )
    val images = (1..3).map { uploadImage() }

    expectToolFailure {
      callTool(
        client,
        "add_key_screenshots",
        mapOf(
          "projectId" to data.projectId,
          "keyScreenshots" to
            listOf(
              mapOf(
                "keyName" to "limited.key",
                "screenshots" to images.map { mapOf("uploadedImageId" to it) },
              ),
            ),
        ),
      )
    }

    // The over-limit batch is rejected before any image is converted, so nothing is attached.
    val key = keyService.find(data.projectId, "limited.key", null)
    assertThat(screenshotService.findAll(key!!)).isEmpty()
  }

  @Test
  fun `add_key_screenshots rejects an image owned by another user`() {
    callTool(
      client,
      "create_keys",
      mapOf("projectId" to data.projectId, "keys" to listOf(mapOf("name" to "idor.key"))),
    )

    val otherData = createTestDataWithPat()
    val otherClient = createMcpClientWithPat(otherData.pat.token!!)
    val foreignImageId =
      callToolAndGetJson(otherClient, "upload_image", mapOf("image" to MINIMAL_PNG_BASE64))["uploadedImageId"].asLong()

    expectToolFailure {
      callTool(
        client,
        "add_key_screenshots",
        mapOf(
          "projectId" to data.projectId,
          "keyScreenshots" to
            listOf(
              mapOf(
                "keyName" to "idor.key",
                "screenshots" to listOf(mapOf("uploadedImageId" to foreignImageId)),
              ),
            ),
        ),
      )
    }

    val key = keyService.find(data.projectId, "idor.key", null)
    assertThat(screenshotService.findAll(key!!)).isEmpty()
  }

  @Test
  fun `add_key_screenshots rejects a non-existent image id`() {
    callTool(
      client,
      "create_keys",
      mapOf("projectId" to data.projectId, "keys" to listOf(mapOf("name" to "noimg.key"))),
    )

    expectToolFailure {
      callTool(
        client,
        "add_key_screenshots",
        mapOf(
          "projectId" to data.projectId,
          "keyScreenshots" to
            listOf(
              mapOf(
                "keyName" to "noimg.key",
                "screenshots" to listOf(mapOf("uploadedImageId" to 9_999_999)),
              ),
            ),
        ),
      )
    }

    val key = keyService.find(data.projectId, "noimg.key", null)
    assertThat(screenshotService.findAll(key!!)).isEmpty()
  }

  @Test
  fun `create_keys without SCREENSHOTS_UPLOAD scope creates no keys`() {
    val restricted =
      createTestDataWithPak(
        // KEYS_CREATE alone permits create_keys but not the screenshot upload it tries to do.
        // (Subtracting SCREENSHOTS_UPLOAD from all scopes would not work: ADMIN re-grants it on expansion.)
        scopes = setOf(Scope.KEYS_CREATE),
        userName = "no_screenshot_scope_user",
        projectName = "no_screenshot_scope_project",
        pakKey = "no_screenshot_scope_pak_key",
      )
    val restrictedClient = createMcpClientWithPak(restricted.apiKey.encodedKey!!)
    val imageId =
      callToolAndGetJson(
        restrictedClient,
        "upload_image",
        mapOf("image" to MINIMAL_PNG_BASE64),
      )["uploadedImageId"].asLong()

    expectToolFailure {
      callTool(
        restrictedClient,
        "create_keys",
        mapOf(
          "projectId" to restricted.projectId,
          "keys" to
            listOf(
              mapOf(
                "name" to "scoped.key",
                "screenshots" to listOf(mapOf("uploadedImageId" to imageId)),
              ),
            ),
        ),
      )
    }

    assertThat(keyService.find(restricted.projectId, "scoped.key", null)).isNull()
  }

  @Test
  fun `upload_image rejects oversized base64`() {
    val oversized = "A".repeat(15_000_001)
    val result = callTool(client, "upload_image", mapOf("image" to oversized))
    assertThat(result.isError).isTrue()
  }

  @Test
  fun `add_key_screenshots associates screenshot with existing key`() {
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "existing.key", "translations" to mapOf("en" to "Existing"))),
      ),
    )

    val uploadJson =
      callToolAndGetJson(
        client,
        "upload_image",
        mapOf("image" to MINIMAL_PNG_BASE64),
      )
    val imageId = uploadJson["uploadedImageId"].asLong()

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
    val imageId = uploadJson["uploadedImageId"].asLong()

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

  private fun uploadImage(): Long =
    callToolAndGetJson(client, "upload_image", mapOf("image" to MINIMAL_PNG_BASE64))["uploadedImageId"].asLong()

  /**
   * A failing tool call may surface either as a thrown JSON-RPC error or as an error
   * [McpSchema.CallToolResult], depending on which layer rejects it. Accept both.
   */
  private fun expectToolFailure(block: () -> McpSchema.CallToolResult) {
    runCatching(block).fold(
      onSuccess = { assertThat(it.isError).isTrue() },
      onFailure = { },
    )
  }
}
