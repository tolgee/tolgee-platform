package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation

class GlossaryTermBuilder(
  val glossaryBuilder: GlossaryBuilder,
) : BaseEntityDataBuilder<GlossaryTerm, GlossaryTermBuilder>() {
  override var self: GlossaryTerm =
    GlossaryTerm().apply {
      glossary = glossaryBuilder.self
      glossaryBuilder.self.terms.add(this)
    }

  class DATA {
    val translations = mutableListOf<GlossaryTermTranslationBuilder>()
  }

  var data = DATA()

  fun addTranslation(ft: FT<GlossaryTermTranslation>) = addOperation(data.translations, ft)
}
