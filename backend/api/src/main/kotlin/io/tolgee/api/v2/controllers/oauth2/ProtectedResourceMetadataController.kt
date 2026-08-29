package io.tolgee.api.v2.controllers.oauth2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.hateoas.oauth2.ProtectedResourceMetadataModel
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.security.oauth2.OAuth2IssuerResolver
import io.tolgee.security.oauth2.OAuth2Scopes
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** RFC 9728 Protected Resource Metadata. Advertises `resource` = the audience tokens carry. */
@RestController
@CrossOrigin(origins = ["*"])
@OpenApiHideFromPublicDocs
@Tag(name = "OAuth2 authorization server")
class ProtectedResourceMetadataController(
  private val issuerResolver: OAuth2IssuerResolver,
) : IController {
  @GetMapping(OAuth2Constants.PROTECTED_RESOURCE_METADATA_PATH)
  @Operation(summary = "RFC 9728 protected-resource metadata for the MCP developer resource")
  fun mcpDeveloperMetadata(): ProtectedResourceMetadataModel {
    // RFC 9728: the path after the well-known prefix is the resource identifier's path, so a client that fetched this
    // URL is asking about <base>/mcp/developer and rejects a document naming anything else. The bare base URL would
    // also collide with the authorization server's own identifier.
    val issuer = issuerResolver.issuerUrl
    return ProtectedResourceMetadataModel(
      resource = issuer + OAuth2Constants.MCP_RESOURCE_PATH,
      authorizationServers = listOf(issuer),
      scopesSupported = OAuth2Scopes.SUPPORTED,
      bearerMethodsSupported = listOf("header"),
    )
  }
}
