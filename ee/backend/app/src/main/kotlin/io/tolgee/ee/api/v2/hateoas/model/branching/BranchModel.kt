package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(itemRelation = "branch", collectionRelation = "branches")
open class BranchModel(
  @Schema(description = "Branch id")
  val id: Long,
  @Schema(description = "Branch name")
  val name: String,
  @Schema(description = "Is branch active")
  val active: Boolean,
  @Schema(description = "Is branch default")
  val isDefault: Boolean,
  ) : RepresentationModel<BranchModel>()
