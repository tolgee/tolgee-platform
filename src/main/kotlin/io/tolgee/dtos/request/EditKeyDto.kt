package io.tolgee.dtos.request

import org.hibernate.validator.constraints.Length
import javax.validation.constraints.NotBlank

data class EditKeyDto(
  @field:NotBlank
  @field:Length(max = 200)
  var name: String = ""
)
