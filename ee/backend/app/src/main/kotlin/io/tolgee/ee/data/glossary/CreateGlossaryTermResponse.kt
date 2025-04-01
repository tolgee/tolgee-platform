package io.tolgee.ee.data.glossary

import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermModel
import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermTranslationModel

data class CreateGlossaryTermResponse(
  val term: GlossaryTermModel,
  val translation: GlossaryTermTranslationModel,
)
