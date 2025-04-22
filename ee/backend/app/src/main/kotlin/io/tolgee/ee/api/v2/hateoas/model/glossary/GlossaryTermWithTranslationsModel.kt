package io.tolgee.ee.api.v2.hateoas.model.glossary

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaryTerms", itemRelation = "glossaryTerm")
class GlossaryTermWithTranslationsModel(
  val id: Long,
  val glossary: GlossaryModel,
  val description: String?,
  val flagNonTranslatable: Boolean,
  val flagCaseSensitive: Boolean,
  val flagAbbreviation: Boolean,
  val flagForbiddenTerm: Boolean,
  val translations: List<GlossaryTermTranslationModel>,
) : RepresentationModel<GlossaryTermWithTranslationsModel>()
