package io.tolgee.security.oauth2

import io.tolgee.mcp.McpConstants
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class OAuth2AudienceTest {
  @Test
  fun `the MCP endpoint and anything under it belong to the MCP audience`() {
    OAuth2Audience.forRequestPath(MCP).assert.isEqualTo(OAuth2Audience.MCP)
    OAuth2Audience.forRequestPath("$MCP/message").assert.isEqualTo(OAuth2Audience.MCP)
  }

  @Test
  fun `a sibling route that merely shares the prefix is not the MCP audience`() {
    OAuth2Audience.forRequestPath("$MCP-tools").assert.isEqualTo(OAuth2Audience.API)
    OAuth2Audience.forRequestPath("${MCP}X").assert.isEqualTo(OAuth2Audience.API)
  }

  @Test
  fun `everything else is the REST API audience`() {
    OAuth2Audience.forRequestPath("/v2/projects").assert.isEqualTo(OAuth2Audience.API)
    OAuth2Audience.forRequestPath("/mcp").assert.isEqualTo(OAuth2Audience.API)
  }

  companion object {
    private const val MCP = McpConstants.DEVELOPER_ENDPOINT_PATH
  }
}
