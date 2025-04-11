package io.tolgee.ee.api.v2.hateoas.model.glossary

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaryTermTranslations", itemRelation = "glossaryTermTranslation")
class GlossaryTermTranslationModel(
  val languageCode: String,
  val text: String,
) : RepresentationModel<GlossaryTermTranslationModel>() {
  companion object {
    fun defaultValue(languageCode: String) = GlossaryTermTranslationModel(languageCode, "")
  }
}
