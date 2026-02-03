package io.tolgee.dtos.request.project

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.request.LanguageRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CreateProjectRequest(
  @field:NotBlank
  @field:Size(min = 3, max = 50)
  var name: String = "",
  @field:NotEmpty
  @field:Valid
  var languages: List<LanguageRequest>? = null,
  @field:Size(min = 3, max = 60)
  @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-]*$", message = "invalid_pattern")
  @Schema(
    description =
      "Slug of your project used in url e.g. \"/v2/projects/what-a-project\"." +
        " If not provided, it will be generated",
  )
  var slug: String? = null,
  @field:Min(1)
  @Schema(description = "Organization to create the project in")
  var organizationId: Long = 0,
  @Schema(
    description =
      "Tag of one of created languages, to select it as base language. If not provided, " +
        "first language will be selected as base.",
  )
  var baseLanguageTag: String? = null,
  @Schema(description = "Whether to use ICU placeholder visualization in the editor and it's support.")
  var icuPlaceholders: Boolean = true,
)
