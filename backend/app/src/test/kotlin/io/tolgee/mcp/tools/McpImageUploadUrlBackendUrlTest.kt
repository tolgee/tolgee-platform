package io.tolgee.mcp.tools

import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

@TestPropertySource(properties = ["tolgee.back-end-url=https://api.example.test"])
class McpImageUploadUrlBackendUrlTest : AbstractMcpTest() {
  @Test
  fun `issued upload URL uses the configured back-end-url`() {
    val data = createTestDataWithPak()
    val client = createMcpClientWithPak(data.apiKey.encodedKey!!)
    val url = callToolAndGetJson(client, "get_image_upload_url")["uploadUrl"].asText()
    assertThat(url).startsWith("https://api.example.test/v2/public/image-upload?token=")
  }
}
