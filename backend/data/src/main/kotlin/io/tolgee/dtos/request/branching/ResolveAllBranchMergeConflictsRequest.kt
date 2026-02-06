package io.tolgee.dtos.request.branching

import io.tolgee.model.enums.BranchKeyMergeResolutionType

data class ResolveAllBranchMergeConflictsRequest(
  val resolve: BranchKeyMergeResolutionType,
)
