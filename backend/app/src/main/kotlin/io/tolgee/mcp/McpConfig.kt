package io.tolgee.mcp

import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpStatelessSyncServer
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.util.VersionProvider
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class McpConfig {
  @Bean
  fun mcpTransport(): WebMvcStatelessServerTransport {
    return WebMvcStatelessServerTransport
      .builder()
      .messageEndpoint(McpConstants.DEVELOPER_ENDPOINT_PATH)
      .build()
  }

  @Bean
  fun mcpServer(
    transport: WebMvcStatelessServerTransport,
    versionProvider: VersionProvider,
    toolsProviders: List<McpToolsProvider>,
  ): McpStatelessSyncServer {
    val server =
      McpServer
        .sync(transport)
        .serverInfo("tolgee", versionProvider.version)
        .capabilities(
          McpSchema.ServerCapabilities
            .builder()
            .tools(false)
            .build(),
        ).immediateExecution(true)
        .build()

    toolsProviders.forEach { it.register(server) }

    return server
  }

  @Bean
  fun mcpRouterFunction(
    transport: WebMvcStatelessServerTransport,
    @Suppress("unused") mcpServer: McpStatelessSyncServer,
  ): RouterFunction<ServerResponse> {
    return transport.routerFunction
  }
}
