package io.tolgee.mcp

import io.tolgee.AbstractMcpTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/** With authentication off every request runs as the initial user, so the cold-start challenge must stay silent. */
@SpringBootTest(
  properties = ["tolgee.authentication.enabled=false"],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class McpAuthDisabledTest : AbstractMcpTest() {
  @Test
  fun `a credential-less tools call is not challenged when authentication is disabled`() {
    val response = postRawJsonRpc(CREDENTIAL_LESS_TOOLS_CALL)

    response.statusCode().assert.isNotEqualTo(401)
    response
      .headers()
      .firstValue("WWW-Authenticate")
      .isPresent.assert
      .isFalse()
  }
}
