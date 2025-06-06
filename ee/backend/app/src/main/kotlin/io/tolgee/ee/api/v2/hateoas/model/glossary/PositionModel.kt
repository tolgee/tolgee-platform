package io.tolgee.ee.api.v2.hateoas.model.glossary

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "positions", itemRelation = "position")
class PositionModel(
  val start: Int,
  val end: Int,
) : RepresentationModel<PositionModel>()
