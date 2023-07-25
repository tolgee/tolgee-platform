package io.tolgee.dtos.request.key

import io.tolgee.constants.ValidationConstants
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class TagKeyDto(
  @field:NotBlank
  @field:Size(max = ValidationConstants.MAX_TAG_LENGTH)
  val name: String = ""
)
