package io.tolgee.ee.data.glossary

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

open class CreateGlossaryTermRequest {
  @Schema(example = "It's trademark", description = "A detailed explanation or definition of the glossary term")
  @field:Size(max = 500)
  var description: String = ""

  @Schema(description = "When true, this term will have the same translation across all target languages")
  var flagNonTranslatable: Boolean = false

  @Schema(description = "When true, the term matching considers uppercase and lowercase characters as distinct")
  var flagCaseSensitive: Boolean = false

  @Schema(description = "Specifies whether the term represents a shortened form of a word or phrase")
  var flagAbbreviation: Boolean = false

  @Schema(description = "When true, marks this term as prohibited or not recommended for use in translations")
  var flagForbiddenTerm: Boolean = false
}
