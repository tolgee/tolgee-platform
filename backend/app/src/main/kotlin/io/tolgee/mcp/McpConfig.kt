package io.tolgee.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpSyncServer
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.util.VersionProvider
import org.redisson.api.RedissonClient
import org.springframework.boot.web.servlet.FilterRegistrationBean
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
    versionProvider: VersionProvider,
    toolsProviders: List<McpToolsProvider>,
  ): McpSyncServer {
    val server =
      McpServer
        .sync(transportProvider)
        .serverInfo("tolgee", versionProvider.version)
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

  @Bean
  fun mcpSessionRedisFilter(
    transportProvider: WebMvcStreamableServerTransportProvider,
    redissonClient: RedissonClient?,
    objectMapper: ObjectMapper,
  ): FilterRegistrationBean<McpSessionRedisFilter> {
    val filter = McpSessionRedisFilter(transportProvider, redissonClient, objectMapper)
    val registration = FilterRegistrationBean(filter)
    registration.addUrlPatterns("/mcp/*")
    return registration
  }
}
