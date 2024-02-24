package io.tolgee.dtos.request.key

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.request.KeyInScreenshotPositionDto

data class KeyScreenshotDto(
  var text: String? = null,
  @Schema(description = "Ids of screenshot uploaded with /v2/image-upload endpoint")
  var uploadedImageId: Long = 0,
  var positions: List<KeyInScreenshotPositionDto>? = null,
)
