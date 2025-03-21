package io.tolgee.ee.data.glossary

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

class CreateGlossaryRequest {
  @field:Size(min = 3, max = 50)
  var name: String = ""

  @field:NotBlank // TODO: if it stays as code we need stricter validation here
  var baseLanguageCode: String? = null

  @field:NotNull
  var assignedProjects: MutableSet<Long>? = null
}
