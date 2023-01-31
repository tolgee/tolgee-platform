package io.tolgee.dtos.request

import javax.validation.constraints.NotEmpty

class KeyDefinitionDto {
  @get:NotEmpty
  var name: String = ""
  var namespace: String? = null
}
