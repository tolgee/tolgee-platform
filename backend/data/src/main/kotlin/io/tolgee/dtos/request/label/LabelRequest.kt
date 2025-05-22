package io.tolgee.dtos.request.label

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length
import jakarta.validation.constraints.Pattern

class LabelRequest {
  @field:NotBlank
  @field:Length(max = 100)
  lateinit var name: String

  @field:Length(max = 2000)
  val description: String? = null

  @Schema(
    description = "Hex color in format #RRGGBB.",
    example = "#FF5733",
  )
  @field:Length(max = 7)
  @field:NotBlank
  @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "hex color required")
  lateinit var color: String
}
