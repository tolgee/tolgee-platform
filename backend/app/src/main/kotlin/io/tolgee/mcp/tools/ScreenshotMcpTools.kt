package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.KeyScreenshotController
import io.tolgee.dtos.request.KeyInScreenshotPositionDto
import io.tolgee.dtos.request.key.KeyScreenshotDto
import io.tolgee.mcp.McpRequestContext
import io.tolgee.mcp.McpToolsProvider
import io.tolgee.mcp.ToolEndpointSpec
import io.tolgee.mcp.buildSpec
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.util.executeInNewTransaction
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.util.Base64

@Component
class ScreenshotMcpTools(
  private val mcpRequestContext: McpRequestContext,
  private val imageUploadService: ImageUploadService,
  private val screenshotService: ScreenshotService,
  private val keyService: KeyService,
  private val projectHolder: ProjectHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val objectMapper: ObjectMapper,
  private val transactionManager: PlatformTransactionManager,
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

  private val addKeyScreenshotsSpec =
    buildSpec(KeyScreenshotController::uploadScreenshot, "add_key_screenshots")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "upload_image",
      "Upload a screenshot image for later use with create_keys or add_key_screenshots. " +
        "Returns an image ID that can be referenced in the screenshots field.",
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

    server.addTool(
      "add_key_screenshots",
      "Add screenshots to existing translation keys. " +
        "First upload images with upload_image, then use this tool to associate them with keys. " +
        "Use this when you need to add screenshots to keys that already exist.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        objectArray("keyScreenshots", "List of keys and their screenshots", required = true) {
          string("keyName", "Translation key name", required = true)
          string("namespace", "Optional: namespace of the key")
          objectArray("screenshots", "Screenshots to associate with this key", required = true) {
            number("uploadedImageId", "Image ID returned by upload_image", required = true)
            objectArray("positions", "Optional: positions of this key's text in the screenshot") {
              number("x", "X coordinate in pixels", required = true)
              number("y", "Y coordinate in pixels", required = true)
              number("width", "Width in pixels", required = true)
              number("height", "Height in pixels", required = true)
            }
          }
        }
        string("branch", "Optional: branch name")
      },
    ) { request ->
      mcpRequestContext.executeAs(addKeyScreenshotsSpec, request.arguments.getProjectId()) {
        val branch = request.arguments.getString("branch")
        val keyScreenshots = request.arguments.requireList("keyScreenshots")

        executeInNewTransaction(transactionManager) {
          // Resolve all keys first — if any is missing, return error before mutations
          val keyScreenshotPairs =
            keyScreenshots.map { entry ->
              val keyName = entry.requireString("keyName")
              val keyEntity =
                keyService.find(projectHolder.project.id, keyName, entry.getString("namespace"), branch)
                  ?: return@executeInNewTransaction errorResult("Key not found: $keyName")
              keyEntity to parseScreenshotDtos(entry.requireList("screenshots"))
            }
          screenshotService.saveUploadedImagesForKeys(keyScreenshotPairs)
          textResult(
            objectMapper.writeValueAsString(
              mapOf("success" to true, "keyCount" to keyScreenshotPairs.size),
            ),
          )
        }
      }
    }
  }
}

/**
 * Parse MCP screenshot arguments into [KeyScreenshotDto] list.
 * Shared by [ScreenshotMcpTools] and [KeyMcpTools].
 */
fun parseScreenshotDtos(screenshots: List<Map<String, Any?>>): List<KeyScreenshotDto> =
  screenshots.map { s ->
    KeyScreenshotDto().apply {
      uploadedImageId = s.requireLong("uploadedImageId")
      positions =
        s.getList("positions")?.map { p ->
          KeyInScreenshotPositionDto(
            x = p.requireInt("x"),
            y = p.requireInt("y"),
            width = p.requireInt("width"),
            height = p.requireInt("height"),
          )
        }
    }
  }
