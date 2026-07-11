package io.tolgee.ee.data.glossary

import io.tolgee.model.glossary.GlossaryTermTranslation

data class GlossaryTermHighlight(
  val position: Position,
  val value: GlossaryTermTranslation,
)
