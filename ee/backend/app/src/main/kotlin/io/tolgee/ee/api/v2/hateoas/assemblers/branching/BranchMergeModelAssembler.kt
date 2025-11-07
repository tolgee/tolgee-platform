package io.tolgee.ee.api.v2.hateoas.assemblers.branching

import io.tolgee.dtos.queryResults.branching.BranchMergeView
import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeModel
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class BranchMergeModelAssembler(
  private val branchModelAssembler: BranchModelAssembler,
) : RepresentationModelAssembler<BranchMergeView, BranchMergeModel> {
  override fun toModel(entity: BranchMergeView): BranchMergeModel {
    return BranchMergeModel(
      id = entity.id,
      sourceBranch = branchModelAssembler.toModel(entity.sourceBranch),
      targetBranch = branchModelAssembler.toModel(entity.targetBranch),
      outdated = !entity.revisionsMatch,
      keyAdditionsCount = entity.keyAdditionsCount.toInt(),
      keyDeletionsCount = entity.keyDeletionsCount.toInt(),
      keyModificationsCount = entity.keyModificationsCount.toInt(),
      keyUnresolvedConflictsCount = entity.keyUnresolvedConflictsCount.toInt(),
      keyResolvedConflictsCount = entity.keyResolvedConflictsCount.toInt(),
    )
  }
}
