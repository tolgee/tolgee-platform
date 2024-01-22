package io.tolgee.dtos.request.screenshot

import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated

@Validated
data class UploadScreenshotDto(
  @field:NotBlank
  var key: String? = null,
)
