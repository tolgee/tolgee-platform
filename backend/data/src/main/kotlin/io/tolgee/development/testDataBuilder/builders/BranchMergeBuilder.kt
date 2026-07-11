package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.FT
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.branching.BranchMergeChange

class BranchMergeBuilder(
  val projectBuilder: ProjectBuilder,
) : BaseEntityDataBuilder<BranchMerge, BranchMergeBuilder>() {
  override val self: BranchMerge = BranchMerge()

  fun addChange(ft: FT<BranchMergeChange>): BranchMergeChangeBuilder {
    return projectBuilder.addBranchMergeChange(this, ft)
  }
}
