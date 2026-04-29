package io.tolgee.ee.data.translationMemory

import io.swagger.v3.oas.annotations.media.Schema

class UpdateProjectTmSettingsRequest {
  @Schema(
    description =
      "When true, only translations whose state is REVIEWED are written to this project's own TM. " +
        "Translations that drop back to TRANSLATED or UNTRANSLATED also remove the entry. " +
        "TMX import and direct TM-browser edits bypass this filter.",
  )
  var writeOnlyReviewed: Boolean = false
}
