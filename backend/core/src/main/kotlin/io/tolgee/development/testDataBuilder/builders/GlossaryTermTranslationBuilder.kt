package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.glossary.GlossaryTermTranslation

class GlossaryTermTranslationBuilder(
  val glossaryTermBuilder: GlossaryTermBuilder,
) : BaseEntityDataBuilder<GlossaryTermTranslation, GlossaryTermTranslationBuilder>() {
  override var self: GlossaryTermTranslation =
    GlossaryTermTranslation(
      languageTag = "en",
    ).apply {
      term = glossaryTermBuilder.self
      glossaryTermBuilder.self.translations.add(this)
    }
}
