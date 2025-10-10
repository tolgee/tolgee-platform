package io.tolgee.ee.api.v2.hateoas.assemblers.branching

import io.tolgee.ee.api.v2.hateoas.model.branching.BranchMergeModel
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.enums.BranchKeyMergeChangeType
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class BranchMergeModelAssembler(
  private val branchModelAssembler: BranchModelAssembler,
) : RepresentationModelAssembler<BranchMerge, BranchMergeModel> {
  override fun toModel(entity: BranchMerge): BranchMergeModel {
    return BranchMergeModel(
      id = entity.id,
      sourceBranch = branchModelAssembler.toModel(entity.sourceBranch),
      targetBranch = branchModelAssembler.toModel(entity.targetBranch),
      keyAdditionsCount = entity.changes.filter { it.change == BranchKeyMergeChangeType.ADD }.size,
      keyDeletionsCount = entity.changes.filter { it.change == BranchKeyMergeChangeType.DELETE }.size,
      keyModificationsCount = entity.changes.filter { it.change == BranchKeyMergeChangeType.UPDATE }.size,
      keyConflictsCount = entity.changes.filter { it.change == BranchKeyMergeChangeType.CONFLICT }.size,
    )
  }
}
