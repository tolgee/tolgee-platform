package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.hateoas.uploadedImage.UploadedImageMcpModel
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.service.ImageUploadService
import io.tolgee.service.mcp.McpImageUploadUrlService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/public/image-upload"])
@Tag(name = "Image upload")
@OpenApiHideFromPublicDocs
class PublicImageUploadController(
  private val mcpImageUploadUrlService: McpImageUploadUrlService,
  private val imageUploadService: ImageUploadService,
) {
  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(
    summary = "Upload an image via a short-lived MCP upload URL",
    description =
      "Unauthenticated. Authorization is the short-lived signed `token` issued by the " +
        "`get_image_upload_url` MCP tool. Returns the `uploadedImageId` to use with create_keys / " +
        "add_key_screenshots.",
  )
  @ResponseStatus(HttpStatus.CREATED)
  fun upload(
    @RequestParam("token") token: String,
    @RequestParam("image") image: MultipartFile,
  ): UploadedImageMcpModel {
    val userAccount = mcpImageUploadUrlService.resolveUserFromUploadToken(token)
    imageUploadService.validateIsImage(image)
    val uploaded = imageUploadService.store(image, userAccount, null)
    return UploadedImageMcpModel(uploadedImageId = uploaded.id)
  }
}
