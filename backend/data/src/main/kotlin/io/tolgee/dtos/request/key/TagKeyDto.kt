package io.tolgee.dtos.request.key

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TagKeyDto(
  @field:NotBlank
  @field:Size(max = 100)
  val name: String = ""
)
