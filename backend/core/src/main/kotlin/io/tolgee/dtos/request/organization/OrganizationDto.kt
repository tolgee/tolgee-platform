package io.tolgee.dtos.request.organization

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class OrganizationDto(
  @field:NotBlank
  @field:Size(min = 3, max = 50)
  @Schema(example = "Beautiful organization")
  var name: String = "",
  @Schema(example = "This is a beautiful organization full of beautiful and clever people")
  var description: String? = null,
  @field:Size(min = 3, max = 60)
  @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-]*$", message = "invalid_pattern")
  @Schema(example = "btforg")
  var slug: String? = null,
)
