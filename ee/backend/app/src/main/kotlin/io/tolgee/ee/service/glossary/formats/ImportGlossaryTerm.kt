package io.tolgee.ee.service.glossary.formats

data class ImportGlossaryTerm(
  val term: String?,
  val description: String?,
  val translations: Map<String, String>,
  var flagNonTranslatable: Boolean?,
  var flagCaseSensitive: Boolean?,
  var flagAbbreviation: Boolean?,
  var flagForbiddenTerm: Boolean?,
)
