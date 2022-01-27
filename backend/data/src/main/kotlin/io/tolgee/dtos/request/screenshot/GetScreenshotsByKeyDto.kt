package io.tolgee.dtos.request.screenshot

import javax.validation.constraints.NotBlank

data class GetScreenshotsByKeyDto(
  @field:NotBlank
  var key: String = ""
)
