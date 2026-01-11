package io.tolgee.dtos.request.screenshot

import jakarta.validation.constraints.NotBlank

data class GetScreenshotsByKeyDto(
  @field:NotBlank
  var key: String = "",
)
