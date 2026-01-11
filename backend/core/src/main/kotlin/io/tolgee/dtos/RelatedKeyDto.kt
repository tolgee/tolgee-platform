package io.tolgee.dtos

import jakarta.validation.constraints.NotEmpty

data class RelatedKeyDto(
  var namespace: String? = null,
  @field:NotEmpty
  var keyName: String = "",
)
