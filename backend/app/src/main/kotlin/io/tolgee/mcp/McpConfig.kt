package io.tolgee.mcp

import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpSyncServer
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider
import io.modelcontextprotocol.spec.McpSchema
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse

@Configuration
class McpConfig {
  @Bean
  fun mcpTransportProvider(): WebMvcStreamableServerTransportProvider {
    return WebMvcStreamableServerTransportProvider
      .builder()
      .mcpEndpoint("/mcp/developer")
      .build()
  }

  @Bean
  fun mcpServer(
    transportProvider: WebMvcStreamableServerTransportProvider,
    buildProperties: BuildProperties,
    toolsProviders: List<McpToolsProvider>,
  ): McpSyncServer {
    val server =
      McpServer
        .sync(transportProvider)
        .serverInfo("tolgee", buildProperties.version ?: "dev")
        .capabilities(
          McpSchema.ServerCapabilities
            .builder()
            .tools(true)
            .build(),
        ).immediateExecution(true)
        .build()

    toolsProviders.forEach { it.register(server) }

    return server
  }

  @Bean
  fun mcpRouterFunction(
    transportProvider: WebMvcStreamableServerTransportProvider,
    @Suppress("unused") mcpServer: McpSyncServer,
  ): RouterFunction<ServerResponse> {
    return transportProvider.routerFunction
  }
}
