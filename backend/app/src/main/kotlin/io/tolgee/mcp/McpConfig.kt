package io.tolgee.mcp

import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.server.McpStatelessSyncServer
import io.modelcontextprotocol.spec.McpSchema
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.oauth2.OAuth2BearerChallengeProvider
import io.tolgee.util.VersionProvider
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.ServerResponse
import tools.jackson.databind.ObjectMapper

/**
 * Serves two tool lists on the same endpoint. An OAuth client (and a client that has not logged in yet, which
 * will most likely log in through OAuth) gets the list without [OAUTH_UNAVAILABLE_TOOLS], so it is never offered
 * a tool that always refuses its token. Every other credential gets the full list.
 */
@Configuration
class McpConfig {
  @Bean
  fun mcpRouterFunction(
    versionProvider: VersionProvider,
    toolsProviders: List<McpToolsProvider>,
    authenticationFacade: AuthenticationFacade,
  ): RouterFunction<ServerResponse> {
    val fullTransport = createTransport()
    val oauthTransport = createTransport()
    createServer(fullTransport, versionProvider, toolsProviders)
    val oauthServer = createServer(oauthTransport, versionProvider, toolsProviders)
    OAUTH_UNAVAILABLE_TOOLS.forEach { oauthServer.removeTool(it) }

    return RouterFunction { request ->
      transportFor(authenticationFacade, fullTransport, oauthTransport).routerFunction.route(request)
    }
  }

  private fun transportFor(
    authenticationFacade: AuthenticationFacade,
    fullTransport: WebMvcStatelessServerTransport,
    oauthTransport: WebMvcStatelessServerTransport,
  ): WebMvcStatelessServerTransport {
    if (authenticationFacade.isOAuthTokenAuth || !authenticationFacade.isAuthenticated) return oauthTransport
    return fullTransport
  }

  private fun createTransport(): WebMvcStatelessServerTransport {
    return WebMvcStatelessServerTransport
      .builder()
      .messageEndpoint(McpConstants.DEVELOPER_ENDPOINT_PATH)
      .build()
  }

  private fun createServer(
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
  fun mcpAuthChallengeFilter(
    challengeProvider: OAuth2BearerChallengeProvider,
    objectMapper: ObjectMapper,
    authenticationProperties: AuthenticationProperties,
  ): FilterRegistrationBean<McpAuthChallengeFilter> {
    val registration =
      FilterRegistrationBean(McpAuthChallengeFilter(challengeProvider, objectMapper, authenticationProperties))
    registration.addUrlPatterns("/mcp/*")
    return registration
  }

  companion object {
    /** Tools that need an organization-level grant, which an OAuth consent cannot give yet. */
    val OAUTH_UNAVAILABLE_TOOLS = setOf("create_project")
  }
}
