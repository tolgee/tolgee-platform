package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.branching.BranchMergeChange

class BranchMergeChangeBuilder(
  val branchMergeBuilder: BranchMergeBuilder,
) : BaseEntityDataBuilder<BranchMergeChange, BranchMergeChangeBuilder>() {
  override val self: BranchMergeChange =
    BranchMergeChange().apply {
      branchMergeBuilder.let { branchMerge = it.self }
    }
}
