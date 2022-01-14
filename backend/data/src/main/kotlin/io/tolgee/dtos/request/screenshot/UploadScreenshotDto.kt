package io.tolgee.dtos.request.screenshot

import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@Validated
data class UploadScreenshotDto(
  @field:NotBlank
  var key: String? = null
)
