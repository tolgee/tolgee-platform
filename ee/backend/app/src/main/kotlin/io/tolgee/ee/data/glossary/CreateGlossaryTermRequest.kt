package io.tolgee.ee.data.glossary

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

open class CreateGlossaryTermRequest {
  @Schema(example = "", description = "Glossary term description")
  @field:Size(max = 500)
  var description: String? = null

  var flagNonTranslatable: Boolean = false

  var flagCaseSensitive: Boolean = false

  var flagAbbreviation: Boolean = false

  var flagForbiddenTerm: Boolean = false
}
