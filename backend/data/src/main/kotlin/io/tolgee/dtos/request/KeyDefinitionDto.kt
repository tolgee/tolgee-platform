package io.tolgee.dtos.request

import jakarta.validation.constraints.NotEmpty

class KeyDefinitionDto {
  @get:NotEmpty
  var name: String = ""
  var namespace: String? = null
}
