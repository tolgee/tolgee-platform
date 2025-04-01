package io.tolgee.ee.api.v2.hateoas.model.glossary

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaryTerms", itemRelation = "glossaryTerm")
class GlossaryTermModel(
  val id: Long,
  val description: String?,
  val flagNonTranslatable: Boolean,
  val flagCaseSensitive: Boolean,
  val flagAbbreviation: Boolean,
  val flagForbiddenTerm: Boolean,
) : RepresentationModel<GlossaryTermModel>()
