package io.tolgee.mcp

import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpStatelessSyncServer
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.mcp.tools.McpToolEffect
import io.tolgee.mcp.tools.McpToolRegistrar
import io.tolgee.mcp.tools.textResult
import io.tolgee.mcp.tools.toolSchema
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport

class McpToolRegistrarTest {
  private val servers = mutableListOf<McpStatelessSyncServer>()

  @AfterEach
  fun closeServers() {
    servers.forEach { it.close() }
  }

  @Test
  fun `the full server gets every tool`() {
    val names = registeredToolNames(oauthOnly = false)

    assertThat(names).isEqualTo(setOf("any_token", "pat_only", "pat_only_opted_in", "org_role"))
  }

  @Test
  fun `the OAuth server gets only tools an OAuth token can run`() {
    val names = registeredToolNames(oauthOnly = true)

    assertThat(names).isEqualTo(setOf("any_token", "pat_only_opted_in"))
  }

  private fun registeredToolNames(oauthOnly: Boolean): Set<String> {
    val server =
      McpServer
        .sync(WebMvcStatelessServerTransport.builder().build())
        .capabilities(
          McpSchema.ServerCapabilities
            .builder()
            .tools(false)
            .build(),
        ).build()
    servers += server
    val registrar = McpToolRegistrar(server, oauthOnly)

    registrar.addTestTool("any_token", spec(AuthTokenType.ANY))
    registrar.addTestTool("pat_only", spec(AuthTokenType.ONLY_PAT))
    registrar.addTestTool("pat_only_opted_in", spec(AuthTokenType.ONLY_PAT).copy(allowsOAuthToken = true))
    registrar.addTestTool("org_role", spec(AuthTokenType.ANY, OrganizationRoleType.OWNER))

    return server.listTools().map { it.name() }.toSet()
  }

  private fun McpToolRegistrar.addTestTool(
    name: String,
    spec: ToolEndpointSpec,
  ) {
    addTool(name, name, McpToolEffect.READ_ONLY, name, toolSchema {}, spec) { textResult("ok") }
  }

  private fun spec(
    allowedTokenType: AuthTokenType,
    requiredOrgRole: OrganizationRoleType? = null,
  ) = ToolEndpointSpec(
    mcpOperation = "test_op",
    requiredScopes = null,
    allowedTokenType = allowedTokenType,
    isWriteOperation = false,
    requiredOrgRole = requiredOrgRole,
  )
}
