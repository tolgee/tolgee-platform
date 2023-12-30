package io.tolgee.dtos.request

import jakarta.validation.constraints.NotBlank

data class GenerateSlugDto(
  @field:NotBlank
  var name: String? = null,
  val oldSlug: String? = null,
)
