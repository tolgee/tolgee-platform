package io.tolgee.ee.api.v2.hateoas.model.glossary

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaryTerms", itemRelation = "glossaryTerm")
class SimpleGlossaryTermWithTranslationsModel(
  val id: Long,
  @Schema(example = "It's trademark", description = "A detailed explanation or definition of the glossary term")
  val description: String,
  @Schema(description = "When true, this term has the same translation across all target languages")
  val flagNonTranslatable: Boolean,
  @Schema(description = "When true, the term matching considers uppercase and lowercase characters as distinct")
  val flagCaseSensitive: Boolean,
  @Schema(description = "Specifies whether the term represents a shortened form of a word or phrase")
  val flagAbbreviation: Boolean,
  @Schema(description = "When true, marks this term as prohibited or not recommended for use in translations")
  val flagForbiddenTerm: Boolean,
  val translations: List<GlossaryTermTranslationModel>,
) : RepresentationModel<SimpleGlossaryTermWithTranslationsModel>()
