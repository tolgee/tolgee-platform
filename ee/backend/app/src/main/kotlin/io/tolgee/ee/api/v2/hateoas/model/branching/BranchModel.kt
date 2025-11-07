package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(itemRelation = "branch", collectionRelation = "branches")
data class BranchModel(
  @Schema(description = "Branch id")
  val id: Long,
  @Schema(description = "Branch name")
  val name: String,
  @Schema(description = "Author of the branch")
  var author: SimpleUserAccountModel? = null,
  @Schema(description = "Is branch active")
  val active: Boolean,
  @Schema(description = "Is branch default")
  val isDefault: Boolean,
  @Schema(description = "Is branch protected")
  val isProtected: Boolean,
  @Schema(description = "Date of branch creation")
  val createdAt: Long? = null,
  ) : RepresentationModel<BranchModel>()
