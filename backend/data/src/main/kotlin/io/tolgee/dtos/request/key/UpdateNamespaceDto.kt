package io.tolgee.dtos.request.key

import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

data class UpdateNamespaceDto(
  @field:NotNull
  @field:NotBlank
  var name: String? = null,
)
