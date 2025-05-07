package io.tolgee.ee.data.glossary

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class CreateGlossaryRequest {
  @Schema(example = "My glossary", description = "Glossary name")
  @field:NotBlank
  @field:Size(max = 50)
  var name: String = ""

  @Schema(example = "cs-CZ", description = "Language tag according to BCP 47 definition")
  @field:NotBlank
  @field:Size(max = 20)
  @field:Pattern(regexp = "^[^,]*$", message = "can not contain coma")
  var baseLanguageTag: String? = null

  @Schema(description = "Projects assigned to glossary")
  @field:NotNull
  var assignedProjects: MutableSet<Long>? = null
}
