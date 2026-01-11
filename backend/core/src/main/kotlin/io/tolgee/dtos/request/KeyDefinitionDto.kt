package io.tolgee.dtos.request

import jakarta.validation.constraints.NotEmpty

data class KeyDefinitionDto(
  @get:NotEmpty
  var name: String = "",
  var namespace: String? = null,
)
