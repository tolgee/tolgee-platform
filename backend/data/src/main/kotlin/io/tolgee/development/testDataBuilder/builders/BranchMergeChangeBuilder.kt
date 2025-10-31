package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.branching.BranchMergeChange

class BranchMergeChangeBuilder(
  val projectBuilder: ProjectBuilder,
  val branchMergeBuilder: BranchMergeBuilder? = null,
) : BaseEntityDataBuilder<BranchMergeChange, BranchMergeChangeBuilder>() {
  override val self: BranchMergeChange = BranchMergeChange().apply {
    branchMergeBuilder?.let { branchMerge = it.self }
  }
}
