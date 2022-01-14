package io.tolgee.dtos.request.project

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.dtos.request.LanguageDto
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class CreateProjectDTO(
  @field:NotBlank @field:Size(min = 3, max = 50)
  var name: String = "",

  @field:NotEmpty
  @field:Valid
  var languages: List<LanguageDto>? = null,

  @field:Size(min = 3, max = 60)
  @field:Pattern(regexp = "^[a-z0-9-]*[a-z]+[a-z0-9-]*$", message = "invalid_pattern")
  @Schema(
    description = "Slug of your project used in url e.g. \"/v2/projects/what-a-project\"." +
      " If not provided, it will be generated"
  )
  var slug: String? = null,

  @Schema(description = "If not provided, project will be created in user scope")
  var organizationId: Long? = null,

  @Schema(
    description = "Tag of one of created languages, to select it as base language. If not provided, " +
      "first language will be selected as base."
  )
  var baseLanguageTag: String? = null
)
