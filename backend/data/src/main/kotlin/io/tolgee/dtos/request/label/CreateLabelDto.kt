package io.tolgee.dtos.request.label

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import jakarta.validation.constraints.Pattern

class CreateLabelDto {
  @field:NotBlank
  @field:Length(max = 100)
  val name: String = ""
  @field:Length(max = 2000)
  val description: String? = null
  @field:Length(max = 7)
  @field:NotBlank
  @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "invalid_pattern")
  val color: String? = null
}
