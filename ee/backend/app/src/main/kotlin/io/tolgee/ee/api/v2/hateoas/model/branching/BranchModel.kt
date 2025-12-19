package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(itemRelation = "branch", collectionRelation = "branches")
data class BranchModel(
  @Schema(description = "Unique identifier of the branch")
  val id: Long,
  @Schema(
    description =
      "Human-readable name of the branch. " +
        "Similar to Git branch names, it identifies the feature or purpose of this branch (e.g. 'feature-login-page')",
  )
  val name: String,
  @Schema(description = "User who created or owns this branch. Can be null for system-generated branches.")
  var author: SimpleUserAccountModel? = null,
  @Schema(
    description =
      "Indicates whether this branch is currently active (visible and usable for editing translations and keys). " +
        "Inactive branches are hidden but still stored in the project.",
  )
  val active: Boolean,
  @Schema(description = "Is branch default")
  val isDefault: Boolean,
  @Schema(description = "Is branch protected")
  val isProtected: Boolean,
  @Schema(description = "Date of branch creation")
  val createdAt: Long? = null,
  @Schema(
    description =
      "Ongoing (or applied) merge operation related to this branch. " +
        "Null when the branch is not being merged yet",
  )
  val merge: BranchMergeRefModel? = null,
  @Schema(description = "Name of the branch this branch was created from")
  val originBranchName: String? = null,
) : RepresentationModel<BranchModel>()
