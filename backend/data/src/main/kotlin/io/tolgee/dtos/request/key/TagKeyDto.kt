package io.tolgee.dtos.request.key

import io.tolgee.constants.ValidationConstants
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class TagKeyDto(
  @field:NotBlank
  @field:Size(max = ValidationConstants.MAX_TAG_LENGTH)
  val name: String = "",
)
