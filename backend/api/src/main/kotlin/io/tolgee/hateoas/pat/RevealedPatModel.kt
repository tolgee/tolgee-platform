package io.tolgee.hateoas.pat

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "pats", itemRelation = "pat")
open class RevealedPatModel(
  @Schema(hidden = true)
  patModel: PatModel,
  val token: String,
) : RepresentationModel<RevealedPatModel>(),
  IPatModel by patModel
