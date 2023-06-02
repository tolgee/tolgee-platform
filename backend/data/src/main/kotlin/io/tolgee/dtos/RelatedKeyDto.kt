package io.tolgee.dtos

import javax.validation.constraints.NotEmpty

data class RelatedKeyDto(
  var namespace: String? = null,

  @field:NotEmpty
  var keyName: String = ""
)
