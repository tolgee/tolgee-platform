package io.tolgee.ee.data.glossary

import io.tolgee.model.glossary.GlossaryTermTranslation

interface GlossaryTermWithTranslationsView {
  val id: Long
  val description: String?
  val flagNonTranslatable: Boolean
  val flagCaseSensitive: Boolean
  val flagAbbreviation: Boolean
  val flagForbiddenTerm: Boolean
  val translations: Set<GlossaryTermTranslation>?
}
