package io.tolgee.ee.data.glossary

import io.tolgee.ee.api.v2.hateoas.model.glossary.GlossaryTermTranslationModel
import io.tolgee.ee.api.v2.hateoas.model.glossary.SimpleGlossaryTermModel

data class CreateUpdateGlossaryTermResponse(
  val term: SimpleGlossaryTermModel,
  val translation: GlossaryTermTranslationModel?,
)
