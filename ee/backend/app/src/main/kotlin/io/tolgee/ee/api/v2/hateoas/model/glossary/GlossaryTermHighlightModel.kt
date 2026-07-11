package io.tolgee.ee.api.v2.hateoas.model.glossary

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaryHighlights", itemRelation = "glossaryHighlight")
class GlossaryTermHighlightModel(
  val position: PositionModel,
  val value: GlossaryTermModel,
) : RepresentationModel<GlossaryTermHighlightModel>()
