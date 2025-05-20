package io.tolgee.dtos.request.label

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

class CreateLabelDto {
  @field:NotBlank
  @field:Length(max = 100)
  val name: String = ""
  @field:Length(max = 2000)
  val description: String? = null
  @field:Length(max = 7)
  @get:NotBlank
  val color: String? = null
}
