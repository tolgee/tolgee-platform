package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.ToolEndpointSpec
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class ScreenshotMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val imageUploadService: ImageUploadService,
  private val authenticationFacade: AuthenticationFacade,
  private val objectMapper: ObjectMapper,
) : McpToolsProvider {
  companion object {
    private const val MAX_BASE64_LENGTH = 15_000_000 // ~10MB image
  }

  private val uploadImageSpec =
    ToolEndpointSpec(
      mcpOperation = "upload_image",
      requiredScopes = null,
      allowedTokenType = AuthTokenType.ANY,
      isWriteOperation = true,
      isGlobalRoute = true,
    )

  override fun register(server: McpSyncServer) {
    server.addTool(
      "upload_image",
      "Upload a screenshot image for later use with create_keys. " +
        "Returns an image ID that can be referenced in the screenshots field of create_keys.",
      toolSchema {
        string("image", "Base64-encoded PNG or JPEG image data", required = true)
      },
    ) { request ->
      mcpRequestContext.executeAs(uploadImageSpec) {
        val base64 = request.arguments.requireString("image")
        if (base64.length > MAX_BASE64_LENGTH) {
          return@executeAs errorResult("Image too large (max ~10MB)")
        }

        val imageBytes =
          try {
            Base64.getDecoder().decode(base64)
          } catch (e: IllegalArgumentException) {
            return@executeAs errorResult("Invalid base64 encoding")
          }

        val resource = ByteArrayResource(imageBytes)
        val imageEntity =
          imageUploadService.store(
            resource,
            authenticationFacade.authenticatedUserEntity,
            null,
          )
        textResult(
          objectMapper.writeValueAsString(
            mapOf(
              "imageId" to imageEntity.id,
            ),
          ),
        )
      }
    }
  }
}
