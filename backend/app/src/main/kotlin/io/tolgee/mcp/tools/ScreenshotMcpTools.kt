package io.tolgee.mcp.tools

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.server.McpSyncServer
import io.tolgee.api.v2.controllers.KeyScreenshotController
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
import io.tolgee.service.mcp.McpImageUploadUrlService
import io.tolgee.service.security.SecurityService
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.getSafeNamespace
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
  private val mcpImageUploadUrlService: McpImageUploadUrlService,
  private val securityService: SecurityService,
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

  private val getImageUploadUrlSpec =
    ToolEndpointSpec(
      mcpOperation = "get_image_upload_url",
      requiredScopes = null,
      allowedTokenType = AuthTokenType.ANY,
      isWriteOperation = true,
      isGlobalRoute = true,
    )

  private val addKeyScreenshotsSpec =
    buildSpec(KeyScreenshotController::uploadScreenshot, "add_key_screenshots")

  override fun register(server: McpSyncServer) {
    server.addTool(
      "get_image_upload_url",
      "(Recommended) Get a short-lived URL for uploading a screenshot image out-of-band, instead of " +
        "sending the image bytes through the model with upload_image. " +
        "Returns { uploadUrl, expiresInSeconds }. Upload the image file to that URL as multipart field " +
        "'image' (e.g. `curl -F image=@<file> \"<uploadUrl>\"`); the response contains an " +
        "'uploadedImageId' to reference in the screenshots field of create_keys or add_key_screenshots.",
      toolSchema { },
    ) { _ ->
      mcpRequestContext.executeAs(getImageUploadUrlSpec) {
        val issued = mcpImageUploadUrlService.issueUploadUrl(authenticationFacade.authenticatedUser.id)
        textResult(
          objectMapper.writeValueAsString(
            mapOf(
              "uploadUrl" to issued.uploadUrl,
              "expiresInSeconds" to issued.expiresInSeconds,
            ),
          ),
        )
      }
    }

    server.addTool(
      "upload_image",
      "(Not recommended — prefer get_image_upload_url, which avoids sending image bytes through the " +
        "model.) Upload a screenshot image for later use with create_keys or add_key_screenshots. " +
        "Returns an uploadedImageId that can be referenced in the screenshots field.",
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
              "uploadedImageId" to imageEntity.id,
            ),
          ),
        )
      }
    }

    server.addTool(
      "add_key_screenshots",
      "Add screenshots to existing translation keys. " +
        "First obtain an uploadedImageId via get_image_upload_url (recommended) or upload_image (legacy " +
        "base64), then use this tool to associate the image(s) with keys. " +
        "Use this when you need to add screenshots to keys that already exist.",
      toolSchema {
        number("projectId", "ID of the project (required for PAT, auto-resolved for PAK)")
        objectArray("keyScreenshots", "List of keys and their screenshots", required = true) {
          string("keyName", "Translation key name", required = true)
          string("namespace", "Optional: namespace of the key")
          screenshotsField("Screenshots to associate with this key", required = true)
        }
        string("branch", "Optional: branch name")
      },
    ) { request ->
      mcpRequestContext.executeAs(addKeyScreenshotsSpec, request.arguments.getProjectId()) {
        val branch = request.arguments.getString("branch")
        val keyScreenshots = request.arguments.requireList("keyScreenshots")

        executeInNewTransaction(transactionManager) {
          val keyScreenshotPairs =
            keyScreenshots.map { entry ->
              val keyName = entry.requireString("keyName")
              val keyEntity =
                keyService.find(
                  projectHolder.project.id,
                  keyName,
                  getSafeNamespace(entry.getString("namespace")),
                  branch,
                )
                  ?: return@executeInNewTransaction errorResult("Key not found: $keyName")
              securityService.checkBranchModify(keyEntity)
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
