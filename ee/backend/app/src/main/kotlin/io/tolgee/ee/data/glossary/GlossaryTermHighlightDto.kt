package io.tolgee.ee.data.glossary

import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermModel

data class GlossaryTermHighlightDto(
  val position: Position,
  val value: GlossaryTermModel,
)
