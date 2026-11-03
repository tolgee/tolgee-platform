package io.tolgee.dtos.request.branching

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.BranchKeyMergeChangeType

data class SetBranchMergeChangesExcludedRequest(
  @Schema(description = "Changes to mark. Null means every change of the merge, narrowed by `type` when given.")
  val changeIds: List<Long>? = null,
  val type: BranchKeyMergeChangeType? = null,
  val excluded: Boolean,
)
