package io.tolgee.ee.api.v2.hateoas.assemblers.branching

import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeRefModel
import io.tolgee.model.branching.BranchMerge
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class BranchMergeRefModelAssembler : RepresentationModelAssembler<BranchMerge, BranchMergeRefModel> {
  override fun toModel(entity: BranchMerge): BranchMergeRefModel {
    return BranchMergeRefModel(
      id = entity.id,
      targetBranchName = entity.targetBranch.name,
      mergedAt = entity.mergedAt?.time,
    )
  }
}
