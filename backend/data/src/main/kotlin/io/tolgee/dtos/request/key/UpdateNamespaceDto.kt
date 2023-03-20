package io.tolgee.dtos.request.key

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UpdateNamespaceDto(
  @field:NotNull
  @field:NotBlank
  var name: String? = null,
)
