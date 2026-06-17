package io.tolgee.mcp.tools

import io.tolgee.AbstractMcpTest
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.security.authentication.JwtService
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.net.URI
import java.time.Duration

@TestPropertySource(properties = ["tolgee.mcp.image-upload-url-expiration-ms=1500"])
class McpImageUploadUrlExpirationTest : AbstractMcpTest() {
  @Autowired lateinit var jwtService: JwtService

  @Test
  fun `sub-second lifetime truncates expiresInSeconds and bounds the token's real expiry`() {
    val data = createTestDataWithPak()
    val client = createMcpClientWithPak(data.apiKey.encodedKey!!)
    val json = callToolAndGetJson(client, "get_image_upload_url")

    assertThat(json["expiresInSeconds"].asLong()).isEqualTo(1L)

    val token = URI(json["uploadUrl"].asText()).query.substringAfter("token=")
    assertDoesNotThrow { jwtService.validateTicket(token, JwtService.TicketType.IMG_UPLOAD) }
    moveCurrentDate(Duration.ofSeconds(2))
    assertThrows<AuthenticationException> { jwtService.validateTicket(token, JwtService.TicketType.IMG_UPLOAD) }
  }
}
