package io.tolgee.ee.data.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class UpdateSharedTranslationMemoryRequest {
  @Schema(example = "Marketing TM", description = "Translation memory name")
  @field:NotBlank
  @field:Size(min = 1, max = 100)
  var name: String = ""

  @Schema(example = "en", description = "Source language tag according to BCP 47 definition")
  @field:NotBlank
  @field:Size(min = 1, max = 20)
  @field:Pattern(regexp = "^[^,]*$", message = "can not contain comma")
  var sourceLanguageTag: String = ""

  @Schema(description = "IDs of projects to assign the TM to. Replaces existing assignments.")
  @Deprecated("Use assignedProjects instead")
  var assignedProjectIds: Set<Long>? = null

  @Schema(
    description =
      "Project assignments with access settings. Replaces existing assignments. " +
        "Takes precedence over assignedProjectIds.",
  )
  var assignedProjects: List<ProjectAssignmentDto>? = null

  @Schema(
    description =
      "Default penalty (0–100) subtracted from match scores for every assignment " +
        "that does not define its own override. Defaults to 0.",
  )
  @field:Min(0)
  @field:Max(100)
  var defaultPenalty: Int? = null

  @Schema(
    description =
      "When true, only translations whose state is REVIEWED are written to this TM. " +
        "Translations that drop back to TRANSLATED or UNTRANSLATED also remove the entry. " +
        "TMX import and direct TM-browser edits bypass this filter. Defaults to false.",
  )
  var writeOnlyReviewed: Boolean? = null
}
