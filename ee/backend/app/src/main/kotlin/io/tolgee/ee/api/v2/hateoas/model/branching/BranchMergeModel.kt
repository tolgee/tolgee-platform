package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(itemRelation = "branchMerge", collectionRelation = "branchMerges")
open class BranchMergeModel(
  @Schema(description = "Branch merge id")
  val id: Long,
  @Schema(description = "Source branch")
  val sourceBranch: BranchModel,
  @Schema(description = "Target branch")
  val targetBranch: BranchModel,
  @Schema(description = "Is merge outdated. If true, it means, that either source or target branch data were changed")
  val outdated: Boolean,
  @Schema(description = "Key additions count")
  val keyAdditionsCount: Int,
  @Schema(description = "Key deletions count")
  val keyDeletionsCount: Int,
  @Schema(description = "Key updates count")
  val keyModificationsCount: Int,
  @Schema(description = "Key unresoled conflicts count")
  val keyUnresolvedConflictsCount: Int,
  @Schema(description = "Key resolved conflicts count")
  val keyResolvedConflictsCount: Int,
) : RepresentationModel<BranchMergeModel>()
