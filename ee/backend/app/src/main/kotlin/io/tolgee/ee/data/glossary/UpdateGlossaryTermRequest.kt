package io.tolgee.ee.data.glossary

import jakarta.validation.constraints.Size

open class UpdateGlossaryTermRequest {
  @field:Size(min = 0, max = 150)
  var description: String? = null

  var flagNonTranslatable: Boolean? = null

  var flagCaseSensitive: Boolean? = null

  var flagAbbreviation: Boolean? = null

  var flagForbiddenTerm: Boolean? = null
}
