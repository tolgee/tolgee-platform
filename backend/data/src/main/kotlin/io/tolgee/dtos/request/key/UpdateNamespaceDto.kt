package io.tolgee.dtos.request.key

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UpdateNamespaceDto(
  var name: String? = null,
  var base: Boolean? = null,
)
