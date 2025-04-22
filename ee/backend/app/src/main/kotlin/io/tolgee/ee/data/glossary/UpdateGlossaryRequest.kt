package io.tolgee.ee.data.glossary

import jakarta.annotation.Nullable
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class UpdateGlossaryRequest {
  @field:Size(min = 3, max = 50)
  var name: String = ""

  @field:NotBlank // TODO: if it stays as code we need stricter validation here
  var baseLanguageTag: String? = null

  /**
   * Assigned projects to glossary.
   * When null, assigned projects will be kept unchanged.
   */
  @field:Nullable
  var assignedProjects: MutableSet<Long>? = null
}
