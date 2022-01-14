package io.tolgee.dtos.request.key

import org.hibernate.validator.constraints.Length
import javax.validation.constraints.NotBlank

data class EditKeyDto(
  @field:NotBlank
  @field:Length(max = 2000)
  var name: String = ""
)
