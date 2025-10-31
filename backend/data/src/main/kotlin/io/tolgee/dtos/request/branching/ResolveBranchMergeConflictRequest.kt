package io.tolgee.dtos.request.branching

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.BranchKeyMergeResolutionType

@Suppress("unused")
open class ResolveBranchMergeConflictRequest(
  @Schema(description = "Merge change id")
  val changeId: Long,
  @Schema(description = "Type of resolution")
  val resolve: BranchKeyMergeResolutionType
)
