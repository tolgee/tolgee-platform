package io.tolgee.dtos.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterAppRequest(
  @field:NotBlank
  @field:Size(max = 255)
  val manifestUrl: String = "",
)
