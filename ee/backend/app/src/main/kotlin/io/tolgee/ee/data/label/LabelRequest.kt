package io.tolgee.ee.data.label

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class LabelRequest {
  @field:NotBlank
  @field:Size(max = 100)
  lateinit var name: String

  @field:Size(max = 2000)
  val description: String? = null

  @Schema(
    description = "Hex color in format #RRGGBB.",
    example = "#FF5733",
  )
  @field:Size(max = 7)
  @field:NotBlank
  @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "hex color required")
  lateinit var color: String
}
