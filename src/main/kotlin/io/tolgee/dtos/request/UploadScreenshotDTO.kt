package io.tolgee.dtos.request

import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@Validated
data class UploadScreenshotDTO(
  @field:NotBlank
  var key: String? = null
)
