package io.tolgee.dtos.request.project

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class EditProjectDTO(
  @field:NotBlank
  @field:Size(min = 3, max = 50)
  var name: String = "",
  @field:Size(min = 3, max = 60)
  @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-]*$", message = "invalid_pattern")
  var slug: String? = null,
  var baseLanguageId: Long? = null,
  @field:Size(min = 3, max = 2000)
  var description: String? = null,
)
