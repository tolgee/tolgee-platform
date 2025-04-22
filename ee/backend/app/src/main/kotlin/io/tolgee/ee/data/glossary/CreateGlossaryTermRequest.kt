package io.tolgee.ee.data.glossary

import jakarta.validation.constraints.Size

open class CreateGlossaryTermRequest {
  @field:Size(min = 0, max = 500)
  var description: String? = null

  var flagNonTranslatable: Boolean = false

  var flagCaseSensitive: Boolean = false

  var flagAbbreviation: Boolean = false

  var flagForbiddenTerm: Boolean = false
}
