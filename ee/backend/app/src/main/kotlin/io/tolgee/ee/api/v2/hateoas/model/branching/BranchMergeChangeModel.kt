package io.tolgee.ee.api.v2.hateoas.model.branching

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.BranchKeyMergeChangeType
import io.tolgee.model.enums.BranchKeyMergeResolutionType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(itemRelation = "branchMergeChange", collectionRelation = "branchMergeChanges")
open class BranchMergeChangeModel(
  @Schema(description = "Branch merge change id")
  val id: Long,
  @Schema(description = "Change type")
  val type: BranchKeyMergeChangeType,
  @Schema(description = "Source branch key")
  val sourceKey: BranchMergeKeyModel?,
  @Schema(description = "Merged branch key (post-merge result)")
  val mergedKey: BranchMergeKeyModel?,
  @Schema(description = "Target branch key")
  val targetKey: BranchMergeKeyModel?,
  @Schema(description = "Languages changed by the merge")
  val changedTranslations: List<String>?,
  @Schema(description = "Type of key conflict resolution")
  val resolution: BranchKeyMergeResolutionType?,
  @Schema(description = "Effective resolution used to compute the merged key")
  val effectiveResolution: BranchKeyMergeResolutionType?,
) : RepresentationModel<BranchMergeChangeModel>()
