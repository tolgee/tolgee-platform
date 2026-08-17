package io.tolgee.api.v2.controllers.oauth2

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.hateoas.oauth2.ProtectedResourceMetadataModel
import io.tolgee.model.enums.Scope
import io.tolgee.security.oauth2.OAuth2AudienceResolver
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** RFC 9728 Protected Resource Metadata. Advertises `resource` = the audience tokens carry. */
@RestController
@CrossOrigin(origins = ["*"])
@Tag(name = "OAuth2 flow")
class ProtectedResourceMetadataController(
  private val audienceResolver: OAuth2AudienceResolver,
) : IController {
  @GetMapping("/.well-known/oauth-protected-resource/mcp/developer")
  @Operation(summary = "RFC 9728 protected-resource metadata for the MCP developer resource")
  fun mcpDeveloperMetadata(): ProtectedResourceMetadataModel {
    val audience = audienceResolver.apiAudience
    return ProtectedResourceMetadataModel(
      resource = audience,
      authorizationServers = listOf(audience),
      scopesSupported = Scope.entries.map { it.value },
      bearerMethodsSupported = listOf("header"),
    )
  }
}
