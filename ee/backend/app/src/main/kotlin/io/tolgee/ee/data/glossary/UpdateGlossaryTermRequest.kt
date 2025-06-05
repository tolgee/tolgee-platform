package io.tolgee.ee.data.glossary

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

open class UpdateGlossaryTermRequest {
  @field:Size(max = 500)
  var description: String? = null

  @Schema(description = "When true, this term will have the same translation across all target languages")
  var flagNonTranslatable: Boolean? = null

  @Schema(description = "When true, the term matching considers uppercase and lowercase characters as distinct")
  var flagCaseSensitive: Boolean? = null

  @Schema(description = "Specifies whether the term represents a shortened form of a word or phrase")
  var flagAbbreviation: Boolean? = null

  @Schema(description = "When true, marks this term as prohibited or not recommended for use in translations")
  var flagForbiddenTerm: Boolean? = null
}
