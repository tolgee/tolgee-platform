package io.tolgee.api.v2.controllers.oauth2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.hateoas.oauth2.ProtectedResourceMetadataModel
import io.tolgee.model.enums.Scope
import io.tolgee.security.oauth2.OAuth2IssuerResolver
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** RFC 9728 Protected Resource Metadata. Advertises `resource` = the audience tokens carry. */
@RestController
@CrossOrigin(origins = ["*"])
@Tag(name = "OAuth2 flow")
class ProtectedResourceMetadataController(
  private val issuerResolver: OAuth2IssuerResolver,
) : IController {
  @GetMapping("/.well-known/oauth-protected-resource$MCP_RESOURCE_PATH")
  @Operation(summary = "RFC 9728 protected-resource metadata for the MCP developer resource")
  fun mcpDeveloperMetadata(): ProtectedResourceMetadataModel {
    // RFC 9728: the path after the well-known prefix is the resource identifier's path, so a client that fetched this
    // URL is asking about <base>/mcp/developer and rejects a document naming anything else. The bare base URL would
    // also collide with the authorization server's own identifier.
    val issuer = issuerResolver.issuerUrl
    return ProtectedResourceMetadataModel(
      resource = issuer + MCP_RESOURCE_PATH,
      authorizationServers = listOf(issuer),
      scopesSupported = Scope.entries.map { it.value },
      bearerMethodsSupported = listOf("header"),
    )
  }

  companion object {
    private const val MCP_RESOURCE_PATH = "/mcp/developer"
  }
}
