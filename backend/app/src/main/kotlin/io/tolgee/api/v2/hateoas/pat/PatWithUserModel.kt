package io.tolgee.api.v2.hateoas.pat

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.user_account.SimpleUserAccountModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "pats", itemRelation = "pat")
open class PatWithUserModel(
  @Schema(hidden = true)
  patModel: PatModel,
  val user: SimpleUserAccountModel
) : IPatModel by patModel, RepresentationModel<PatWithUserModel>()
